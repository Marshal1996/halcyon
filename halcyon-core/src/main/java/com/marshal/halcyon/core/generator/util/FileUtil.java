package com.marshal.halcyon.core.generator.util;


import com.marshal.halcyon.core.component.SessionContext;
import com.marshal.halcyon.core.controller.BaseController;
import com.marshal.halcyon.core.entity.BaseEntity;
import com.marshal.halcyon.core.entity.ResponseData;
import com.marshal.halcyon.core.generator.config.ThymeLeafConfig;
import com.marshal.halcyon.core.generator.dto.DBColumn;
import com.marshal.halcyon.core.generator.dto.DBTable;
import com.marshal.halcyon.core.generator.dto.FtlInfo;
import com.marshal.halcyon.core.generator.dto.GeneratorInfo;
import com.marshal.halcyon.core.generator.dto.XmlColumnsInfo;
import com.marshal.halcyon.core.service.BaseService;
import com.marshal.halcyon.core.service.impl.BaseServiceImpl;
import com.marshal.halcyon.core.util.RequestHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
public class FileUtil {


    private FileUtil() {

    }

    public enum pType {
        Controller, MapperXml, Mapper, Service, Impl, Html
    }

    private static List<String> allClassFiles = new ArrayList<>();
    private static List<String> allXmlFiles = new ArrayList<>();
    private static List<String> allHtmlFiles = new ArrayList<>();


    public static String columnToCamel(String str) {
        Pattern linePattern = Pattern.compile("_(\\w)");
        str = str.toLowerCase();
        Matcher matcher = linePattern.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
        }
        matcher.appendTail(sb);
        String s = sb.toString();
        return s;
    }

    public static String camelToColumn(String str) {
        return str.replaceAll("[A-Z]", "_$0").toLowerCase();
    }

    public static void createDto(DBTable table, GeneratorInfo generatorInfo) throws IOException {
        // key 是否需要引入相对包
        boolean needUtil = false;
        boolean needNotNull = false;
        boolean needNotEmpty = false;

        String name = generatorInfo.getDtoName().substring(0, generatorInfo.getDtoName().indexOf("."));
        String projectPath = generatorInfo.getProjectPath();
        String parentPackagePath = generatorInfo.getParentPackagePath();
        String packagePath = generatorInfo.getPackagePath();
        String directory = projectPath + "/src/main/java/" + parentPackagePath + "/" + packagePath + "/entity/"
                + generatorInfo.getDtoName();

        File file = new File(directory);
        createFileDir(file);
        file.createNewFile();
        List<DBColumn> columns = table.getColumns();
        // 判断是否需要引入包
        for (DBColumn s : columns) {
            switch (s.getJdbcType().toUpperCase()) {
                case "DATE":
                case "TIMESTAMP":
                case "DATETIME":
                case "TIME":
                    s.setJavaType("Date");
                    s.setJdbcType("DATE");
                    needUtil = true;
                    break;
                case "BIGINT":
                case "INT":
                case "INTEGER":
                case "DECIMAL":
                case "TINYINT":
                    s.setJavaType("Long");
                    s.setJdbcType("DECIMAL");
                    break;
                case "DOUBLE":
                    s.setJavaType("Double");
                    s.setJdbcType("DECIMAL");
                    break;
                case "NUMBER":
                case "FLOAT":
                    s.setJavaType("Float");
                    s.setJdbcType("DECIMAL");
                    break;
                default:
                    s.setJavaType("String");
                    s.setJdbcType("VARCHAR");
                    break;
            }
            if (s.isNotNull()) {
                needNotNull = true;
            } else if (s.isNotEmpty()) {
                needNotEmpty = true;
            }
        }
        // 生成属性
        StringBuilder sb = new StringBuilder();
        String dir1 = parentPackagePath + "/" + packagePath + "/entity";
        dir1 = dir1.replaceAll("/", ".");
        sb.append("package " + dir1 + ";\r\n\r\n");
        sb.append("import javax.persistence.GeneratedValue;\r\n");
        sb.append("import javax.persistence.Id;\r\n");
        sb.append("import org.hibernate.validator.constraints.Length;\r\n");
        sb.append("import javax.persistence.Table;\r\n");
        String d = BaseEntity.class.getName();
        sb.append("import " + d + ";\r\n");
        if (needUtil) {
            sb.append("import java.util.Date;\r\n");
        }
        if (needNotNull) {
            sb.append("import javax.validation.constraints.NotNull;\r\n");
        }
        if (needNotEmpty) {
            sb.append("import org.hibernate.validator.constraints.NotEmpty;\r\n");
        }
        sb.append("@Table(name = " + "\"" + table.getName() + "\")\r\n");
        sb.append("public class " + name + " extends BaseEntity {\r\n\r\n");

        // 生成属性
        for (DBColumn cl : columns) {
            if (cl.isId()) {
                sb.append("     @Id\r\n");
            } else {
                if (cl.isNotEmpty()) {
                    sb.append("     @NotEmpty\r\n");
                } else if (cl.isNotNull()) {
                    sb.append("     @NotNull\r\n");
                }
                if (cl.getJavaType().equalsIgnoreCase("String")) {
                    sb.append("     @Length(max = ");
                    sb.append(cl.getColumnLength() + ")\r\n");
                }
            }
            String str = "     private " + cl.getJavaType() + " " + columnToCamel(cl.getName()) + ";";
            if (!StringUtils.isEmpty(cl.getRemarks())) {
                str += " //" + cl.getRemarks();
            }
            str += "\r\n\r\n";
            sb.append(str);
        }
        // 生成get 和 set方法
        sb.append("\r\n");
        for (DBColumn cl : columns) {
            String name1 = columnToCamel(cl.getName());
            String name2 = name1.substring(0, 1).toUpperCase() + name1.substring(1);
            sb.append("     public void set" + name2 + "(" + cl.getJavaType() + " " + name1 + "){\r\n");
            sb.append("         this." + name1 + " = " + name1 + ";\r\n");
            sb.append("     }\r\n\r\n");
            sb.append("     public " + cl.getJavaType() + " get" + name2 + "(){\r\n");
            sb.append("         return " + name1 + ";\r\n");
            sb.append("     }\r\n\r\n");
        }
        sb.append("     }\r\n");

        PrintWriter p = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "utf-8"));
        p.write(sb.toString());
        p.close();

    }

    public static void createMapperXml(DBTable table, GeneratorInfo generatorInfo) throws IOException {

        String parentPackagePath = generatorInfo.getParentPackagePath();
        String projectPath = generatorInfo.getProjectPath();
        String packagePath = generatorInfo.getPackagePath();
        String directory = projectPath + "/src/main/resources/mapper/" + generatorInfo.getMapperXmlName();
        String dtoName = generatorInfo.getDtoName().substring(0, generatorInfo.getDtoName().indexOf('.'));
        String packageName = parentPackagePath.replaceAll("/", ".") + "." + packagePath;
        String mapperNameSpace = generatorInfo.getMapperXmlName().substring(0, generatorInfo.getMapperXmlName().indexOf('.'));
        File file = new File(directory);
        createFileDir(file);
        file.createNewFile();
        List<DBColumn> columns = table.getColumns();
        for (DBColumn s : columns) {

            // 生成属性
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
            sb.append("<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\r\n");
            sb.append("<mapper namespace=\"" + packageName + ".mapper." + mapperNameSpace + "\">\r\n");
            sb.append("<resultMap id=\"BaseResultMap\" type=\"" + packageName + ".entity." + dtoName + "\">\r\n");

            // 生成属性
            for (DBColumn cl : columns) {
                String str = "        <result column=\"" + cl.getName() + "\" property=\"" + columnToCamel(cl.getName()) + "\" jdbcType=\"" + cl.getJdbcType() + "\" />\r\n";
                if (cl.isId()) {
                    str = "        <id column=\"" + cl.getName() + "\" property=\"" + columnToCamel(cl.getName()) + "\" jdbcType=\"" + cl.getJdbcType() + "\" />\r\n";
                }
                sb.append(str);
            }
            sb.append("    </resultMap>\r\n");
            sb.append("</mapper>");

            PrintWriter p = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "utf-8"));
            p.write(sb.toString());
            p.close();
        }
    }

    public static void createFtlInfoByType(pType type, DBTable table, GeneratorInfo generatorInfo) throws IOException {
        String projectPath = generatorInfo.getProjectPath();
        String parentPackagePath = generatorInfo.getParentPackagePath();
        String packagePath = generatorInfo.getPackagePath();
        String htmlModelName = generatorInfo.getHtmlModelName();
        String pac = parentPackagePath + "/" + packagePath;
        FtlInfo info = new FtlInfo();
        String directory = null;
        List<String> importPackages = new ArrayList<>();
        if (type == pType.Controller) {
            directory = projectPath + "/src/main/java/" + pac + "/controller/" + generatorInfo.getControllerName();
            importPackages.add(BaseController.class.getName());
            importPackages.add(SessionContext.class.getName());
            importPackages.add(RequestHelper.class.getName());
            importPackages.add(ResponseData.class.getName());
        } else if (type == pType.Mapper) {
            directory = projectPath + "/src/main/java/" + pac + "/mapper/" + generatorInfo.getMapperName();
            importPackages.add("tk.mybatis.mapper.common.Mapper");
        } else if (type == pType.Service) {
            directory = projectPath + "/src/main/java/" + pac + "/service/" + generatorInfo.getServiceName();
            importPackages.add(BaseService.class.getName());
        } else if (type == pType.Impl) {
            directory = projectPath + "/src/main/java/" + pac + "/service/impl/" + generatorInfo.getImplName();
            importPackages.add(BaseServiceImpl.class.getName());
            importPackages.add("org.springframework.stereotype.Service");
        }
        pac = pac.replaceAll("/", ".");
        info.setPackageName(pac);
        info.setDir(directory);
        info.setProjectPath(projectPath);
        info.setImportName(importPackages);
        info.setHtmlModelName(htmlModelName);
        List<DBColumn> columns = table.getColumns();

        List<XmlColumnsInfo> columnsInfos = new ArrayList<>();

        for (DBColumn column : columns) {
            XmlColumnsInfo columnsInfo = new XmlColumnsInfo();
            columnsInfo.setTableColumnsName(columnToCamel(column.getName()));
            columnsInfo.setdBColumnsName(column.getName());
            columnsInfo.setJdbcType(column.getJdbcType());
            columnsInfos.add(columnsInfo);
        }
        info.setColumnsInfo(columnsInfos);
        createFtl(info, type, generatorInfo);
    }

    public static void createFtl(FtlInfo ftlInfo, pType type, GeneratorInfo generatorInfo) throws IOException {
        TemplateEngine templateEngine = ThymeLeafConfig.getTemplateEngine();
        Context context = new Context();
        Map<String, Object> map = new HashMap<String, Object>();
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest();

        String templateName = null;
        if (type == pType.Controller) {
            templateName = "controller.java";
        } else if (type == pType.Mapper) {
            templateName = "mapper.java";
        } else if (type == pType.Service) {
            templateName = "service.java";
        } else if (type == pType.Impl) {
            templateName = "serviceImpl.java";
        }
        if (templateName != null) {
            File file = new File(ftlInfo.getDir());
            createFileDir(file);
            FileWriter writer = new FileWriter(file);
//            OutputStream out = new FileOutputStream(file);
            map.put("package", ftlInfo.getPackageName());
            map.put("import", ftlInfo.getImportName());
            map.put("name", ftlInfo.getFileName());
            map.put("dtoName", generatorInfo.getDtoName().substring(0, generatorInfo.getDtoName().indexOf('.')));
            map.put("controllerName",
                    generatorInfo.getControllerName().substring(0, generatorInfo.getControllerName().indexOf('.')));
            map.put("implName", generatorInfo.getImplName().substring(0, generatorInfo.getImplName().indexOf('.')));
            map.put("serviceName",
                    generatorInfo.getServiceName().substring(0, generatorInfo.getServiceName().indexOf('.')));
            map.put("mapperName", generatorInfo.getMapperName().substring(0, generatorInfo.getMapperName().indexOf('.')));
            map.put("xmlName",
                    generatorInfo.getMapperXmlName().substring(0, generatorInfo.getMapperXmlName().indexOf('.')));
            map.put("columnsInfo", ftlInfo.getColumnsInfo());
            String url = generatorInfo.getTargetName().toLowerCase();
            url = url.replaceAll("_", "/");
            map.put("queryUrl", "/" + url + "/query");
            map.put("submitUrl", "/" + url + "/save");
            map.put("removeUrl", "/" + url + "/delete");
            map.forEach((k, v) -> {
                context.setVariable(k, v);
            });
            writer.write(templateEngine.process(templateName, context));
            writer.flush();
            writer.close();
        }
    }

    // 判断文件目录是否存在不存在则创建
    public static void createFileDir(File file) throws IOException {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
    }

    // 判断文件是否已经存在
    public static int isFileExist(GeneratorInfo generatorInfo) {
        int rs = 0;
        String classDir = generatorInfo.getProjectPath() + "/src/main/java/" + generatorInfo.getParentPackagePath();
        String xmlDir = generatorInfo.getProjectPath() + "/src/main/resources/" + generatorInfo.getParentPackagePath();
        getFileList(classDir, classDir, generatorInfo);
        // 判断有没有重复的java文件

        for (String name : allClassFiles) {
            if (name.equals(generatorInfo.getDtoName())) {
                if ("Create".equalsIgnoreCase(generatorInfo.getDtoStatus())) {
                    rs = 1;
                    break;
                } else if ("Cover".equalsIgnoreCase(generatorInfo.getDtoStatus())) {
                    File file1 = new File(
                            classDir + "/" + generatorInfo.getPackagePath() + "/entity/" + generatorInfo.getDtoName());
                    if (!file1.exists()) {
                        rs = 1;
                        break;
                    }
                }
            }
        }

        if (rs == 0) {
            for (String name : allClassFiles) {
                if (name.equals(generatorInfo.getServiceName())) {
                    if ("Create".equalsIgnoreCase(generatorInfo.getServiceStatus())) {
                        rs = 2;
                        break;
                    } else if ("Cover".equalsIgnoreCase(generatorInfo.getServiceStatus())) {
                        File file1 = new File(classDir + "/" + generatorInfo.getPackagePath() + "/service/"
                                + generatorInfo.getServiceName());
                        if (!file1.exists()) {
                            rs = 2;
                            break;
                        }
                    }
                }
            }
        }

        if (rs == 0) {
            for (String name : allClassFiles) {
                if (name.equals(generatorInfo.getImplName())) {
                    if ("Create".equalsIgnoreCase(generatorInfo.getImplStatus())) {
                        rs = 3;
                        break;
                    } else if ("Cover".equalsIgnoreCase(generatorInfo.getImplStatus())) {
                        File file1 = new File(classDir + "/" + generatorInfo.getPackagePath() + "/service/impl/"
                                + generatorInfo.getImplName());
                        if (!file1.exists()) {
                            rs = 3;
                            break;
                        }
                    }
                }
            }
        }

        if (rs == 0) {
            for (String name : allClassFiles) {
                if (name.equals(generatorInfo.getControllerName())) {
                    if ("Create".equalsIgnoreCase(generatorInfo.getControllerStatus())) {
                        rs = 4;
                        break;
                    } else if ("Cover".equalsIgnoreCase(generatorInfo.getControllerStatus())) {
                        File file1 = new File(classDir + "/" + generatorInfo.getPackagePath() + "/controllers/"
                                + generatorInfo.getControllerName());
                        if (!file1.exists()) {
                            rs = 4;
                            break;
                        }
                    }
                }
            }
        }
        if (rs == 0) {
            for (String name : allClassFiles) {
                if (name.equals(generatorInfo.getMapperName())) {
                    if ("Create".equalsIgnoreCase(generatorInfo.getMapperStatus())) {
                        rs = 5;
                        break;
                    } else if ("Cover".equalsIgnoreCase(generatorInfo.getMapperStatus())) {
                        File file1 = new File(classDir + "/" + generatorInfo.getPackagePath() + "/mapper/"
                                + generatorInfo.getMapperName());
                        if (!file1.exists()) {
                            rs = 5;
                            break;
                        }
                    }
                }
            }
        }

        // 判断有没有重复的xml文件
        if (rs == 0) {
            getFileList(xmlDir, xmlDir, generatorInfo);
            for (String name : allXmlFiles) {
                if (name.equals(generatorInfo.getMapperXmlName())) {
                    if ("Create".equalsIgnoreCase(generatorInfo.getMapperXmlStatus())) {
                        rs = 6;
                        break;
                    } else if ("Cover".equalsIgnoreCase(generatorInfo.getMapperXmlStatus())) {
                        File file1 = new File(xmlDir + "/" + generatorInfo.getPackagePath() + "/mapper/"
                                + generatorInfo.getMapperXmlName());
                        if (!file1.exists()) {
                            rs = 6;
                            break;
                        }
                    }
                }
            }
        }
        return rs;
    }

    // 获取文件夹下所有文件列表
    public static void getFileList(String basePath, String directory, GeneratorInfo generatorInfo) {
        File dir = new File(basePath);
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                String fileName = files[i].getName();
                if (files[i].isDirectory()) {
                    getFileList(files[i].getAbsolutePath(), directory, generatorInfo);
                } else {
                    if (directory.equals(generatorInfo.getProjectPath() + "/src/main/java/"
                            + generatorInfo.getParentPackagePath())) {
                        allClassFiles.add(fileName);
                    } else if (directory.equals(generatorInfo.getProjectPath() + "/src/main/resources/"
                            + generatorInfo.getParentPackagePath())) {
                        allXmlFiles.add(fileName);
                    } else if (directory.equals(generatorInfo.getProjectPath() + "/src/main/webapp/WEB-INF/view")) {
                        allHtmlFiles.add(fileName);
                    }
                }
            }
        }

    }

}
