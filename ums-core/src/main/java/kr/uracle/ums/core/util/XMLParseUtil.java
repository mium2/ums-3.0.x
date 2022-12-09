package kr.uracle.ums.core.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
@SuppressWarnings("unchecked")
public class XMLParseUtil {
	public static Map<String, Object> XmlToMap(String filePath) {
		Document doc = null;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(filePath);
			
			Element root = doc.getDocumentElement();
			
			return getChild(root);
		} catch (SAXException e) {
		} catch (IOException e) {
		} catch (ParserConfigurationException e) {
		}
		return new HashMap<String, Object>();
	}
	private static Map<String, Object> getChild(Node node) {
		Map<String, Object> map = new HashMap<String, Object>();
		
		NodeList childs = node.getChildNodes();
		if (childs.getLength() > 1) {
			Map<String, Object> cmap = new HashMap<String, Object>();
			for (int i = 0; i < childs.getLength(); i++) {
				if (childs.item(i).getNodeType() == Node.TEXT_NODE) continue;
				
				Object obj = cmap.get(childs.item(i).getNodeName());
				if (cmap.get(childs.item(i).getNodeName()) != null) {
					if (obj instanceof ArrayList) {
						((ArrayList) obj).add(getChild(childs.item(i)).get(childs.item(i).getNodeName()));
					} else {
						List<Object> list = new ArrayList<Object>();
						list.add(obj);
						list.add(getChild(childs.item(i)).get(childs.item(i).getNodeName()));
						
						cmap.put(childs.item(i).getNodeName(), list);
					}
				} else {
					cmap.putAll(getChild(childs.item(i)));
				}
			}
			map.put(node.getNodeName(), cmap);
		} else {
			map.put(node.getNodeName(), node.getTextContent());
		}
		
		return map;
	}
}