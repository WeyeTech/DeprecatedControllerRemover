package test;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TestCleanup {
    
    // This field is used
    private final String usedField = "Hello World";
    
    // This field is unused and should be removed
    private final String unusedField = "This should be removed";
    
    // This field has an annotation, so it should be preserved
    @Deprecated
    private final String annotatedField = "This should be preserved";
    
    // This field is not final, so it should be preserved
    private String nonFinalField = "This should be preserved";
    
    // This field is not private, so it should be preserved
    public final String publicField = "This should be preserved";
    
    public void testMethod() {
        // Use the used field
        System.out.println(usedField);
        
        // Use List and ArrayList (imports should be preserved)
        List<String> list = new ArrayList<>();
        list.add("test");
        
        // Use HashMap (import should be preserved)
        HashMap<String, String> map = new HashMap<>();
        map.put("key", "value");
        
        // Use Set (import should be preserved)
        Set<String> set = map.keySet();
        
        // Use File (import should be preserved)
        File file = new File("test.txt");
        
        // Use Path (import should be preserved)
        Path path = file.toPath();
        
        // Use LocalDateTime and DateTimeFormatter (imports should be preserved)
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        String formatted = now.format(formatter);
        
        // IOException is imported but not used - should be removed
        // This line is just a comment to show the unused import
    }
} 