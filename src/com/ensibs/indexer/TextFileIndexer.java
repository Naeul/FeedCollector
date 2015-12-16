package com.ensibs.indexer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
/**
 * This terminal application creates an Apache Lucene index in a folder and adds files into this index
 * based on the input of the user.
 */


import com.ensibs.object.RSSObject;

/**
 * 
 * SNOWBALL
 *
 */
public class TextFileIndexer {

	//private StandardAnalyzer analyzer = new StandardAnalyzer();
	private Analyzer analyzer = new MyAnalyzer();
	private IndexWriter writer;
	private String indexLocation;

	/**
	 * Constructor
	 * @param indexDir the name of the folder in which the index should be created
	 * @throws java.io.IOException when exception creating index.
	 */
	@SuppressWarnings("deprecation")
	public TextFileIndexer(String indexDir){
		// the boolean true parameter means to create a new index everytime, 
		// potentially overwriting any existing files there.
		FSDirectory dir;
		this.indexLocation = indexDir;
		try{
			dir = FSDirectory.open(new File(indexDir));
			IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_45, analyzer);
			writer = new IndexWriter(dir, config);
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	public void index(HashMap<String,RSSObject> myMap){
		for(Entry<String,RSSObject> entry : myMap.entrySet()){
			RSSObject myObject = entry.getValue();
			Document doc = new Document();			
			//===================================================
			// add contents of file
			//===================================================
			doc.add(new TextField("pid", myObject.getPid(), Field.Store.YES));
			doc.add(new TextField("title", myObject.getTitle(), Field.Store.YES));
			doc.add(new TextField("linkSource", myObject.getLinkSource(), Field.Store.YES));
			doc.add(new TextField("linkPage", myObject.getLinkPage(), Field.Store.YES));
			doc.add(new TextField("lastUpdate", myObject.getLastUpdate()+"", Field.Store.YES));
			doc.add(new TextField("description", myObject.getDescription(), Field.Store.YES));
			doc.add(new TextField("content", myObject.getContent(), Field.Store.YES));
			doc.add(new TextField("language", myObject.getLanguage(), Field.Store.YES));
			doc.add(new TextField("streamCategory", myObject.getStreamCategory(), Field.Store.YES));
			doc.add(new TextField("predictCategory", myObject.getPredictCategory(), Field.Store.YES));
			doc.add(new TextField("lastCheck", myObject.getLastCheck()+"", Field.Store.YES));			

			try {
				writer.addDocument(doc);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			this.closeIndex();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public HashMap<String, RSSObject> readDB() {
		HashMap<String, RSSObject> map = new HashMap<String, RSSObject>();

		try {
			IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexLocation)));
			RSSObject object = null;
			System.out.println("nb docs : "+reader.numDocs());
			for(int i = 0; i < reader.numDocs(); i++){
				object = new RSSObject(
						reader.document(i).getField("pid").stringValue(), 
						reader.document(i).getField("title").stringValue(), 
						reader.document(i).getField("linkSource").stringValue(), 
						reader.document(i).getField("linkPage").stringValue(), 
						reader.document(i).getField("lastUpdate").stringValue(), 
						reader.document(i).getField("description").stringValue(), 
						reader.document(i).getField("content").stringValue(), 
						reader.document(i).getField("language").stringValue(), 
						reader.document(i).getField("streamCategory").stringValue(), 
						reader.document(i).getField("predictCategory").stringValue(), 
						reader.document(i).getField("lastCheck").stringValue());
				map.put(reader.document(i).getField("pid").stringValue(), object);
			}
			reader.close();
		} catch (IOException e) {}
		return map;
	}

	/**
	 * Search a string in the documents indexed
	 * @param string
	 * @throws IOException 
	 */
	public void searchIndex(String searchString) throws IOException {
		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexLocation)));
		IndexSearcher searcher = new IndexSearcher(reader);
		
		try {
			System.out.println("Query="+searchString);
			MultiFieldQueryParser queryParser = new MultiFieldQueryParser(new String[]{"title","description","content"}, analyzer);
			Query query = queryParser.parse(searchString);
			TopDocs hits =searcher.search(query, 10);
			// 4. display results

			for(int i=0;i<hits.scoreDocs.length;++i) {
				int docId = hits.scoreDocs[i].doc;
				Document d = searcher.doc(docId);
				System.out.print((i + 1) + ". " + d.get("title") + " score=" + hits.scoreDocs[i].score + " : ");
				System.out.print(hits.scoreDocs[i].score + " / " + hits.getMaxScore() + " = " + hits.scoreDocs[i].score / hits.getMaxScore());
				if(hits.scoreDocs[i].score / hits.getMaxScore()>0.50){
					System.out.print(" Good Score");
				}
				System.out.println();				
			}
		} catch (Exception e) {
			System.out.println("Error searching " + searchString + " : " + e.getMessage());
		}
	}


	/**
	 * Close the index.
	 * @throws java.io.IOException when exception closing
	 */
	public void closeIndex() throws IOException {
		writer.close();
	}
}