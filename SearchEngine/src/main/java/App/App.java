package App;

import java.io.IOException;
import java.util.Scanner;

import org.apache.lucene.queryparser.classic.ParseException;

public class App {

    public static void main(String[] args)
            throws IOException, ParseException {
        Scanner scanner = new Scanner(System.in);

        SearchEngine searchEngine = new SearchEngine(selectAlgorithm(scanner));
        searchEngine.buildIndex(args);
        if (shouldRunAllQueries(scanner))
            searchEngine.runAllQueries();
        else
            searchEngine.queryLoop(scanner);
        searchEngine.shutdown();
        scanner.close();
    }

    static SearchEngine.ScoringAlgorithm selectAlgorithm(Scanner scanner) {

        SearchEngine.ScoringAlgorithm algorithm = null;
        while (algorithm == null) {
            System.out.println(
                    "Select scoring method:\n"
                            + "[a]\tClassic Similarity\n"
                            + "[b]\tBM25 Similarity\n"
                            + "[c]\tBoolean Similarity\n"
                            + "[d]\tLM Dirichlet Similarity\n"
                            + "[e]\tDFI Similarity\n");
            String userResponse = scanner.nextLine();
            switch (userResponse) {
                case "a":
                    algorithm = SearchEngine.ScoringAlgorithm.Classic;
                    break;
                case "b":
                    algorithm = SearchEngine.ScoringAlgorithm.BM25;
                    break;
                case "c":
                    algorithm = SearchEngine.ScoringAlgorithm.Boolean;
                    break;
                case "d":
                    algorithm = SearchEngine.ScoringAlgorithm.LMDirichlet;
                    break;
                case "e":
                    algorithm = SearchEngine.ScoringAlgorithm.DFISimilarity;
                    break;
                default:
                    break;
            }
        }
        return algorithm;
    }

    static boolean shouldRunAllQueries(Scanner scanner) {
        boolean allQueries = false;
        boolean modeChosen = false;
        while (!modeChosen) {
            System.out.println(
                    "Choose query mode:\n"
                            + "[a]\tRun all Cranfield queries.\n"
                            + "[b]\tType query.\n");
            String userResponse = scanner.nextLine();
            switch (userResponse) {
                case "a":
                    allQueries = true;
                    modeChosen = true;
                    break;
                case "b":
                    modeChosen = true;
                    break;
                default:
                    break;
            }
        }
        return allQueries;
    }

}
