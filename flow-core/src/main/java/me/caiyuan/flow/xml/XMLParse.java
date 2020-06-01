package me.caiyuan.flow.xml;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ryan
 * Time: 2015/08/08 22:09
 */
public class XMLParse {

    @SafeVarargs
    public static XML parse(String xml, Map<String, String>... argument) throws XMLParseException {
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setIgnoringComments(true);
            builderFactory.setIgnoringElementContentWhitespace(false);
            DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();

            Document document;
            Map<String, String> values = argument.length > 0 ? argument[0] : null;
            if (values != null && values.size() > 0) {
                InputStream stream = replace(xml, values);
                document = documentBuilder.parse(stream);
            } else {
                document = documentBuilder.parse(xml);
            }
            Element element = document.getDocumentElement();
            return traverse(element);

        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new XMLParseException(e);
        }
    }

    private static InputStream replace(String xml, Map<String, String> values) throws IOException {
        BufferedReader reader = new LineNumberReader(new FileReader(xml));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        String body = builder.toString();

        Pattern p = Pattern.compile("\\$\\{[^}]+}?");
        Matcher m = p.matcher(body);

        StringBuilder content = new StringBuilder();
        int begin = 0;
        while (m.find()) {
            String group = m.group();
            int start = m.start();
            int end = m.end();

            String key = group.substring(2, group.length() - 1);
            content.append(body, begin, start);
            String value = values.get(key);
            if (value == null) {
                value = group;
            }
            content.append(value);
            begin = end;
        }
        int end = body.length();
        content.append(body, begin, end);

        return new ByteArrayInputStream(content.toString().getBytes());
    }

    private static XML traverse(Node element) {

        XML xml = new XML();

        xml.setNode(element.getNodeName());

        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            xml.putAttribute(attr.getNodeName(), attr.getNodeValue());
        }

        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            switch (node.getNodeType()) {
                case Node.ELEMENT_NODE:
                    XML c = traverse(node);
                    c.setFather(xml);
                    xml.addChild(c);
                    break;
                case Node.TEXT_NODE:
                    String value = node.getNodeValue().trim();
                    if (value.equals(""))
                        continue;
                    xml.setText(value);
                    break;
            }
        }

        return xml;
    }

}


