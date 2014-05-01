package file_watcher;


import org.w3c.dom.*;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

public class XMLReader {
    private Config config = new Config();
    private Source source = new Source();
    private static Logger logger = Logger.getLogger(XMLReader.class);
    private List<Destination> destinations = new ArrayList<Destination>();



    /**
     * @throws Exception
     */
    public Config read() throws Exception {
         boolean config_ok = false;

        while(config_ok == false){
            try {

                File file = new File("config/config.xml");

                DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder();

                Document doc = dBuilder.parse(file);

                logger.debug("Root element :" + doc.getDocumentElement().getNodeName());



                source.setPattern(this.readElement(doc, "/instance/inbound/source/pattern"));
                source.setDirectory(Paths.get(this.readElement(doc, "/instance/inbound/source/directory")));

                XPath xPath = XPathFactory.newInstance().newXPath();
                NodeList nodes = (NodeList)xPath.evaluate("/instance/outbound/destination",
                        doc.getDocumentElement(), XPathConstants.NODESET);
                for (int i = 0; i < nodes.getLength(); ++i) {
                    Destination destination = new Destination();

                    Document newDocument = dBuilder.newDocument();
                    Node node = newDocument.importNode(nodes.item(i),true);
                    newDocument.appendChild(node);
                    destination.setPattern(this.readElement(newDocument, "/destination/pattern"));
                    destination.setDirectory(Paths.get(this.readElement(newDocument, "/destination/directory")));
                    destinations.add(destination);
                }
                config.setDestinations(destinations);
                config.setSource(source);
                config_ok = true;


            } catch (Exception e) {
                logger.error(e.getMessage());
                logger.error("Error reading configuration, waiting 10 seconds ...");
                Thread.sleep(10000);
                //e.printStackTrace();
            }
        }

        return config;
    }

    private String readElement( Document doc, String path){
        String value = null;
        XPath xPath = XPathFactory.newInstance().newXPath();

        try {
            NodeList nodes = (NodeList)xPath.evaluate(path,
                    doc.getDocumentElement(), XPathConstants.NODESET);
            if(nodes.getLength() == 1) {
                value = nodes.item(0).getTextContent();
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }

        logger.debug(value);
        return value;

    }



}