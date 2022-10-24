package App;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.DFISimilarity;
import org.apache.lucene.search.similarities.IndependenceStandardized;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class SearchEngine {

	private static String INDEX_DIRECTORY = "../index";
	private static String RESULTS_FILE = "../results/out.txt";
	private static String QUERY_FILE = "../cran/cran.qry";

	public enum ScoringAlgorithm {
		BM25,
		Classic,
		Boolean,
		LMDirichlet,
		DFISimilarity
	}

	private Analyzer analyzer;
	private Directory directory;
	private DirectoryReader ireader;
	private IndexSearcher isearcher;
	private ScoringAlgorithm selectedAlgorithm;

	public SearchEngine(ScoringAlgorithm algorithm) throws IOException {
		this.analyzer = new EnglishAnalyzer();
		this.directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
		this.selectedAlgorithm = algorithm;

	}

	private void createSearcher() throws IOException {
		ireader = DirectoryReader.open(directory);
		isearcher = new IndexSearcher(ireader);
		switch (selectedAlgorithm) {
			case BM25:
				isearcher.setSimilarity(new BM25Similarity());
				break;
			case Classic:
				isearcher.setSimilarity(new ClassicSimilarity());
				break;
			case Boolean:
				isearcher.setSimilarity(new BooleanSimilarity());
			case LMDirichlet:
				isearcher.setSimilarity(new LMDirichletSimilarity());
			case DFISimilarity:
				isearcher.setSimilarity(new DFISimilarity(new IndependenceStandardized()));
				break;
		}
	}

	public void buildIndex(String[] args) throws IOException {

		FieldType vectorField = new FieldType(TextField.TYPE_STORED);
		vectorField.setTokenized(true);
		vectorField.setStoreTermVectors(true);
		vectorField.setStoreTermVectorPositions(true);
		vectorField.setStoreTermVectorOffsets(true);
		vectorField.setStoreTermVectorPayloads(true);

		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		IndexWriter iwriter = new IndexWriter(directory, config);

		populateIndex(args, iwriter, vectorField);
		System.out.println("close");
		iwriter.close();
		createSearcher();
	}

	private void populateIndex(String[] corpus, IndexWriter iwriter, FieldType ft) {
		ArrayList<Document> documents = new ArrayList<Document>();

		System.out.printf("Indexing \"%s\"\n", corpus[0]);
		String content;
		try {
			content = new String(Files.readAllBytes(Paths.get(corpus[0])));
			String[] items = content.split(".I (?=[0-9]+)");
			System.out.println("size: " + items.length);
			for (String item : items) {
				if (!item.equals(""))
					documents.add(this.processItem(item, ft));
			}
			iwriter.addDocuments(documents);

		} catch (IOException e) {
			System.out.println("Error Ocurred while reading files:\n" + e);
		}
	}

	Document processItem(String item, FieldType ft) throws IOException {
		Document doc = new Document();
		String[] fields = item.split(".[TAWB](\r\n|[\r\n])", -1);
		doc.add(new StringField("index", fields[0].trim(), Field.Store.YES));
		doc.add(new StringField("filename", fields[1].trim(), Field.Store.YES));
		doc.add(new StringField("author(s)", fields[2].trim(), Field.Store.YES));
		doc.add(new StringField("metadata", fields[3].trim(), Field.Store.YES));
		doc.add(new Field("content", fields[4].trim(), ft));

		return doc;
	}

	public static List<String> analyze(String text, Analyzer analyzer, String fieldName) throws IOException {
		List<String> result = new ArrayList<String>();
		TokenStream tokenStream = analyzer.tokenStream(fieldName, text);
		CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
		tokenStream.reset();
		while (tokenStream.incrementToken()) {
			result.add(attr.toString());
		}
		tokenStream.close();
		return result;
	}

	public ScoreDoc[] sendQuery(String queryString,
			boolean print)
			throws ParseException, IOException {
		List<String> tmpterms = analyze(queryString, analyzer, "content");
		if (print)
			System.out.println("terms:");
		BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
		for (String term : tmpterms) {
			if (print)
				System.out.print(term + " ");
			Query queryTerm = new TermQuery(new Term("content", term));
			booleanQuery.add(queryTerm, BooleanClause.Occur.SHOULD);
		}
		if (print)
			System.out.println();

		return isearcher.search(booleanQuery.build(), 30).scoreDocs;

	}

	public void runAllQueries()
			throws IOException, ParseException {
		File Fileright = new File(RESULTS_FILE);
		PrintWriter writer = new PrintWriter(RESULTS_FILE, "UTF-8");

		String content = new String(Files.readAllBytes(Paths.get(QUERY_FILE)));
		String[] items = content.split(".I (?=[0-9]+[\n\r]+)");

		int j = 0;

		for (String item : items) {

			item = item.trim();

			if (item.length() > 0) {

				String[] sections = item.split(".W(\r\n|[\r\n])");

				ScoreDoc[] hits = sendQuery(sections[1], false);
				if (hits.length == 0)
					System.out.println("fail");
				for (int i = 0; i < hits.length; i++) {
					Document hitDoc = isearcher.doc(hits[i].doc);

					writer.println(
							j + " 0 " + hitDoc.get("index") + " " + (i + 1) + " "
									+ hits[i].score
									+ " STANDARD");
				}

			}
			j++;
		}
		writer.close();

	}

	public void queryLoop(
			Scanner scanner)
			throws IOException, ParseException {
		boolean shouldQuit = false;
		String userInput;
		System.out.println("Type 'q' to quit.");

		while (!shouldQuit) {
			System.out.print(">>> ");
			userInput = scanner.nextLine();

			if (userInput.length() > 0) {
				if (userInput.equals("q")) {
					shouldQuit = true;
				} else {
					ScoreDoc[] hits = sendQuery(userInput, false);
					if (hits.length == 0)
						System.out.println("Sorry, no documents matched that query.");
					for (int i = 0; i < hits.length; i++) {
						Document hitDoc = isearcher.doc(hits[i].doc);

						System.out.println(i + ": " + hitDoc.get("filename"));
					}
				}
				System.out.println();

			}
		}
	}

	public void shutdown() throws IOException {
		directory.close();
	}
}
