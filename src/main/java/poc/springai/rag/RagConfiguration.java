package poc.springai.rag;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@Configuration
public class RagConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RagConfiguration.class);

    @Value("vectorstore.json")
    private String vectorStoreName;

    @Value("${vectorDataPath}")
    String vectorDataFolder;

    @Value("classpath:/docs")
    private Resource docs;

    @Autowired
    private ResourceLoader resourceLoader;

    @Bean
    SimpleVectorStore simpleVectorStore(EmbeddingModel embeddingModel) throws IOException {

        var simpleVectorStore = new SimpleVectorStore(embeddingModel);
        var vectorStoreFile = getVectorStoreFile();
        if (vectorStoreFile.exists()) {
            log.info("Vector Store File Exists," + vectorStoreFile);
            simpleVectorStore.load(vectorStoreFile);
        } else {
            log.info("Vector Store File Does Not Exist, loading documents");
            List<Document> splitDocuments = getDocuments(docs);
            simpleVectorStore.add(splitDocuments);
            simpleVectorStore.save(vectorStoreFile);
        }
        return simpleVectorStore;
    }

    private List<Document> getDocuments(Resource resource) {
        List<Document> splitDocuments = new ArrayList<>();
        List<File> allFilesInResourceFolder = getAllFilesInResourceFolder(resource);

        allFilesInResourceFolder.forEach(file -> {

            TikaDocumentReader documentReader = new TikaDocumentReader(new FileSystemResource(file));
            List<Document> documents = documentReader.get();
            TextSplitter textSplitter = new TokenTextSplitter();
            splitDocuments.addAll(textSplitter.apply(documents));
        });

        return splitDocuments;
    }

    public List<File> getAllFilesInResourceFolder(Resource resource) {
        //File folder = ResourceUtils.getFile("classpath:" + folderPath);
        List<File> files = new ArrayList<>();
        try {
            File folder = resource.getFile();

            if (folder.isDirectory()) {
                Files.walk(folder.toPath())
                        .filter(Files::isRegularFile)
                        .forEach(path -> files.add(path.toFile()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return files;
    }

    private List<Document> getDocuments2(Resource file) {
        TextReader textReader = new TextReader(file);
        //textReader.getCustomMetadata().put("filename", "olympic-faq.txt");
        //textReader.getCustomMetadata().put("filename", "co_create.sql");
        List<Document> documents = textReader.get();
        TextSplitter textSplitter = new TokenTextSplitter();
        List<Document> splitDocuments = textSplitter.apply(documents);
        return splitDocuments;
    }


    private File getVectorStoreFile() {

        //create folder if not exists
        File file = new File(vectorDataFolder);
        boolean created = file.mkdir();
        if (created) {
            log.info("created vector file location:" + vectorDataFolder);
        }
        return new File(vectorDataFolder + "/" + vectorStoreName);
    }

}
