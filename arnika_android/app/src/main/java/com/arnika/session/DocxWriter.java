package com.arnika.session;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class DocxWriter {
    private DocxWriter(){}
    private static String esc(String s){return s==null?"":s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
    private static void put(ZipOutputStream z,String n,String d)throws Exception{z.putNextEntry(new ZipEntry(n));z.write(d.getBytes(StandardCharsets.UTF_8));z.closeEntry();}
    public static byte[] create(String title,String text)throws Exception{
        ByteArrayOutputStream b=new ByteArrayOutputStream();ZipOutputStream z=new ZipOutputStream(b);
        put(z,"[Content_Types].xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>");
        put(z,"_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/></Relationships>");
        StringBuilder p=new StringBuilder("<w:p><w:pPr><w:bidi/></w:pPr><w:r><w:rPr><w:b/></w:rPr><w:t>").append(esc(title)).append("</w:t></w:r></w:p>");
        for(String line:(text==null?"":text).split("\\n",-1))p.append("<w:p><w:pPr><w:bidi/></w:pPr><w:r><w:rPr><w:rtl/></w:rPr><w:t xml:space=\"preserve\">").append(esc(line)).append("</w:t></w:r></w:p>");
        put(z,"word/document.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>"+p+"<w:sectPr/></w:body></w:document>");
        z.finish();z.close();return b.toByteArray();
    }
}
