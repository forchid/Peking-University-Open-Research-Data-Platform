/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.edu.pku.lib.dataverse.doi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author luopc
 */
@Stateless
public class DOIDataCiteRegisterService {
    private static final Logger logger = Logger.getLogger(DOIDataCiteRegisterService.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    private String url;
    private String username;
    private String password;
    
    DataCiteRESTfullClient client;
    
    public DOIDataCiteRegisterService() {
        url = System.getProperty("doi.baseurlstring");
        username = System.getProperty("doi.username");
        password = System.getProperty("doi.password"); 
        client = new DataCiteRESTfullClient(url,username,password);
    }
    
    @PreDestroy
    public void close(){
        client.close();
    }

    public String createIdentifier(String identifier, HashMap<String, String> metadata) {
        DataCiteMetadataTemplate metadataTemplate = new DataCiteMetadataTemplate();
        metadataTemplate.setIdentifier(identifier.substring(identifier.indexOf(':')+1));
        metadataTemplate.setCreators(Util.getListFromStr(metadata.get("datacite.creator")));
        metadataTemplate.setTitle(metadata.get("datacite.title"));
        metadataTemplate.setPublisher(metadata.get("datacite.publisher"));
        metadataTemplate.setPublisherYear(metadata.get("datacite.publicationyear"));
        logger.log(Level.INFO, "identifier={0}, creators={1}, title={2}, publisher={3}, publicationyear={4}",
                new Object[]{identifier, metadata.get("datacite.creator"), 
                    metadata.get("datacite.title"), metadata.get("datacite.publisher"),
                    metadata.get("datacite.publicationyear")});
        String xmlMetadata = metadataTemplate.generateXML();
        String status = metadata.get("_status").trim();
        String target = metadata.get("_target");
        String retString = "";
        if(status.equals("reserved")){
            DOIDataCiteRegisterCache rc = findByDOI(identifier);
            if(rc == null){
                rc = new DOIDataCiteRegisterCache();
                rc.setDoi(identifier);
                rc.setXml(xmlMetadata);
                rc.setStatus("reserved");
                rc.setUrl(target);
                em.persist(rc);
            }else{
                rc.setDoi(identifier);
                rc.setXml(xmlMetadata);
                rc.setStatus("reserved");
                rc.setUrl(target);
            }
            retString = "success to reserved "+identifier;
        }else if(status.equals("public")){
            DOIDataCiteRegisterCache rc = findByDOI(identifier);
            if(rc != null){
                rc.setDoi(identifier);
                rc.setXml(xmlMetadata);
                rc.setStatus("public");
                if(target == null || target.trim().length()==0){
                    target = rc.getUrl();
                }else{
                    rc.setUrl(target);
                }
                retString = client.postMetadata(xmlMetadata);  
                client.postUrl(identifier.substring(identifier.indexOf(":")+1), target);              
            }
        }else if(status.equals("unavailable")){
            DOIDataCiteRegisterCache rc = findByDOI(identifier);
            if(rc != null){
                rc.setStatus("unavailable");
                retString = client.inactiveDataset(identifier.substring(identifier.indexOf(":")+1));
            }
        }
        return retString;
    }

    public HashMap<String, String> getMetadata(String identifier) {
        HashMap<String,String> metadata = new HashMap();
        try{
            String xmlMetadata = client.getMetadata(identifier.substring(identifier.indexOf(":")+1));
            DataCiteMetadataTemplate template = new DataCiteMetadataTemplate(xmlMetadata);
            metadata.put("datacite.creator", Util.getStrFromList(template.getCreators()));
            metadata.put("datacite.title", template.getTitle());
            metadata.put("datacite.publisher", template.getPublisher());
            metadata.put("datacite.publicationyear", template.getPublisherYear());
            DOIDataCiteRegisterCache rc = findByDOI(identifier);
            if(rc != null){
                metadata.put("_status",rc.getStatus());
            }
        }catch(RuntimeException e){
            logger.log(Level.INFO, identifier, e);
        }
        return metadata;
    }
    
    public DOIDataCiteRegisterCache findByDOI(String doi){
        Query query = em.createNamedQuery("DOIDataCiteRegisterCache.findByDoi",
                    DOIDataCiteRegisterCache.class);
        query.setParameter("doi", doi);
        List<DOIDataCiteRegisterCache> rc = query.getResultList();
        if(rc.size() == 1){
            return rc.get(0);
        }
        return null;
    }
    
    public void deleteIdentifier(String identifier){
        DOIDataCiteRegisterCache rc = findByDOI(identifier);
        if(rc != null){
            em.remove(rc);
        }
    }
    
}

class DataCiteMetadataTemplate {

    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.DataCiteMetadataTemplate");
    private static String template;

    static {
        try (InputStream in = DataCiteMetadataTemplate.class.getResourceAsStream("datacite_metadata_template.xml")) {
            template = Util.readAndClose(in, "utf-8");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "datacite metadata template load error");
            logger.log(Level.SEVERE, "String " + e.toString());
            logger.log(Level.SEVERE, "localized message " + e.getLocalizedMessage());
            logger.log(Level.SEVERE, "cause " + e.getCause());
            logger.log(Level.SEVERE, "message " + e.getMessage());
        }
    }
    
    private String xmlMetadata;
    private String identifier;
    private List<String> creators;
    private String title;
    private String publisher;
    private String publisherYear;
    
    public DataCiteMetadataTemplate(){
    }
    
    public DataCiteMetadataTemplate(String xmlMetaData){
        this.xmlMetadata = xmlMetaData;
        Document doc = Jsoup.parseBodyFragment(xmlMetaData);
        Elements identifierElements = doc.select("identifier");
        if(identifierElements.size()>0)
            identifier = identifierElements.get(0).html();
        Elements creatorElements = doc.select("creatorName");
        creators = new ArrayList();
        for(Element creatorElement : creatorElements){
            creators.add(creatorElement.html());
        }
        Elements titleElements = doc.select("title");
        if(titleElements.size()>0)
            title = titleElements.get(0).html();
        Elements publisherElements = doc.select("publisher");
        if(publisherElements.size()>0)
            publisher = publisherElements.get(0).html();
        Elements publisherYearElements = doc.select("publicationYear");
        if(publisherYearElements.size()>0)
            publisherYear = publisherYearElements.get(0).html();
    }

    public String generateXML() {
        xmlMetadata = template.replace("${identifier}", this.identifier.trim())
                .replace("${title}", this.title)
                .replace("${publisher}", this.publisher)
                .replace("${publisherYear}", this.publisherYear);
        StringBuilder creatorsElement = new StringBuilder();
        for (String creator : creators) {
            creator = creator.trim();
            if (creator.length() > 0) {
                creatorsElement.append("<creator><creatorName>");
                creatorsElement.append(creator);
                creatorsElement.append("</creatorName></creator>");
            }
        }
        xmlMetadata = xmlMetadata.replace("${creators}", creatorsElement.toString());
        return xmlMetadata;
    }

    public static String getTemplate() {
        return template;
    }

    public static void setTemplate(String template) {
        DataCiteMetadataTemplate.template = template;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public List<String> getCreators() {
        return creators;
    }

    public void setCreators(List<String> creators) {
        this.creators = creators;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getPublisherYear() {
        return publisherYear;
    }

    public void setPublisherYear(String publisherYear) {
        this.publisherYear = publisherYear;
    }
}

class Util {

    public static void close(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                throw new RuntimeException("Fail to close InputStream");
            }
        }
    }

    public static String readAndClose(InputStream inStream, String encoding) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buf = new byte[128];
        String data;
        try {
            int cnt;
            while ((cnt = inStream.read(buf)) >= 0) {
                outStream.write(buf, 0, cnt);
            }
            data = outStream.toString(encoding);
        } catch (IOException ioe) {
            throw new RuntimeException("IOException");
        } finally {
            close(inStream);
        }
        return data;
    }
    
    public static List<String> getListFromStr(String str){
        return Arrays.asList(str.split("; "));
//        List<String> authors = new ArrayList();
//        int preIdx = 0;
//        for(int i=0;i<str.length();i++){
//            if(str.charAt(i)==';'){
//                authors.add(str.substring(preIdx,i).trim());
//                preIdx = i+1;
//            }
//        }
//        return authors;
    }
    public static String getStrFromList(List<String> authors){
        StringBuilder str = new StringBuilder();
        for(String author : authors){
            if(str.length()>0)str.append("; ");
            str.append(author);
        }
        return str.toString();
    }
}
