package App;

import java.io.IOException;

import org.apache.lucene.queryparser.classic.ParseException;

public class App {

    public static void main(String[] args)
            throws IOException, ParseException {

        SearchEngine searchEngine = new SearchEngine();
        searchEngine.buildIndex(args);
        searchEngine.runAllQueries();
        searchEngine.shutdown();

    }
}
