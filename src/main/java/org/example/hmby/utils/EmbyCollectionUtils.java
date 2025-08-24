package org.example.hmby.utils;

import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
public class EmbyCollectionUtils {

    /**
     * 方法1: 扫描目录下的 .nfo 文件，返回文件内容和媒体文件是否存在
     */
    public static List<Map<String, Object>> scanNfoFiles(String folderPath) throws Exception {
        File dir = new File(folderPath);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalArgumentException("目录不存在: " + folderPath);
        }

        List<Map<String, Object>> result = new ArrayList<>();

        File[] nfoFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".nfo"));
        if (nfoFiles == null) return result;

        for (File nfo : nfoFiles) {
            String content = Files.readString(Paths.get(nfo.getAbsolutePath()));

            // 媒体文件检查：假设 .mp4 / .mkv / .avi 等格式
            String baseName = nfo.getName().replaceFirst("[.][^.]+$", "");
            boolean mediaExists = Arrays.stream(new String[]{"mp4", "mkv", "avi"})
                    .map(ext -> new File(dir, baseName + "." + ext))
                    .anyMatch(File::exists);

            Map<String, Object> map = new HashMap<>();
            map.put("file", nfo.getAbsolutePath());
            map.put("content", content);
            map.put("mediaExists", mediaExists);

            result.add(map);
        }

        return result;
    }

    /**
     * 方法2: 批量处理目录下的 .nfo 文件，添加/更新 XML 数据
     */
    public static void updateCollectionInfoByFolder(String folderPath, String collectionName) throws Exception {
        File dir = new File(folderPath);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalArgumentException("目录不存在: " + folderPath);
        }

        File[] nfoFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".nfo"));
        if (nfoFiles == null) return;

        for (File nfo : nfoFiles) {
            updateCollectionInfoByFile(nfo.getAbsolutePath(), collectionName);
        }
    }

    /**
     * 方法3: 更新单个 .nfo 文件，添加/更新 set 和 tag
     */
    public static void updateCollectionInfoByFile(String nfoPath, String collectionName) throws Exception {
        File file = new File(nfoPath);
        if (!file.exists()) {
            throw new IllegalArgumentException("文件不存在: " + nfoPath);
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(file);
        doc.getDocumentElement().normalize();

        Element movie = (Element) doc.getElementsByTagName("movie").item(0);
        if (movie == null) {
            throw new IllegalStateException("不是合法的 NFO 文件: " + nfoPath);
        }

        // 1. 更新/新增 <set><name>合集</name></set>
        NodeList setList = movie.getElementsByTagName("set");
        if (setList.getLength() > 0) {
            movie.removeChild(setList.item(0)); // 删除旧的
        }
        Element set = doc.createElement("set");
        Element name = doc.createElement("name");
        name.setTextContent(collectionName);
        set.appendChild(name);
        movie.appendChild(set);

        // 2. 更新/新增 <tag>
        NodeList tags = movie.getElementsByTagName("tag");
        boolean tagExists = false;
        for (int i = 0; i < tags.getLength(); i++) {
            if (collectionName.equals(tags.item(i).getTextContent())) {
                tagExists = true;
                break;
            }
        }
        if (!tagExists) {
            // 先删除同值 tag（覆盖逻辑）
            for (int i = 0; i < tags.getLength(); i++) {
                if (collectionName.equals(tags.item(i).getTextContent())) {
                    movie.removeChild(tags.item(i));
                }
            }
            Element tag = doc.createElement("tag");
            tag.setTextContent(collectionName);
            movie.appendChild(tag);
        }

        // 写回文件
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(doc), new StreamResult(file));

        log.info("已更新: {}", nfoPath);
    }

}
