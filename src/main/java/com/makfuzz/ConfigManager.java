package com.makfuzz;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.*;
import java.io.File;

public class ConfigManager {

    @XmlRootElement(name = "MakFuzzConfig")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class AppConfig {
        public String sourcePath = "";
        public double globalThreshold = 0.3;
        public int topN = 1000;
        public String language = "en";
        
        @XmlElementWrapper(name = "SelectedCriteria")
        @XmlElement(name = "Criteria")
        public java.util.List<CriteriaConfig> criteriaList = new java.util.ArrayList<>();

        @XmlElementWrapper(name = "AvailableColumns")
        @XmlElement(name = "Column")
        public java.util.List<ColumnConfig> availableColumns = new java.util.ArrayList<>();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ColumnConfig {
        public String name = "";
        public int index = -1;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CriteriaConfig {
        public String value = "";
        public String type = "SIMILARITY"; 
        public double spellingWeight = 1.0;
        public double phoneticWeight = 1.0;
        public double minSpelling = 0.8;
        public double minPhonetic = 0.8;
    }

    public static void saveConfig(AppConfig config, File file) {
        try {
            JAXBContext context = JAXBContext.newInstance(AppConfig.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.marshal(config, file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static AppConfig loadConfig(File file) {
        if (!file.exists()) return null;
        try {
            JAXBContext context = JAXBContext.newInstance(AppConfig.class);
            Unmarshaller um = context.createUnmarshaller();
            return (AppConfig) um.unmarshal(file);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
