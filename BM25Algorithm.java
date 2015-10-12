

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BM25Algorithm {


    /* This data structure will store my inverted list in the following format.
     * (word, <<DocID1, TF> ,<DocID2, TF> ... >) ...*/
    private static LinkedHashMap<String, LinkedHashMap<Integer, Integer>> wordList = 
            new LinkedHashMap();

    /* This data structure will store all documents with it's length in the following format.
       (DocID, length) ... */
    private static LinkedHashMap<Integer, Integer> docCount = new LinkedHashMap();

    /* This will store query with all documets in their BM25 Socre in the following format.
       (Query, <<DocID1, BM25Score>,<DocID2,BM25Score>...>)...*/
    private static LinkedHashMap<String, LinkedHashMap<Integer, Double>> finalScore = 
            new LinkedHashMap();
    
    private static String indexFilePath, qryFilePath;
    private static double avdl;
    private static int docLimit;
    private static String ARBITRARY_QNAME_CONSTANT = "q0";
    private static String SYS_NAME_CONSTANT = "System_Name";
    private static final Logger logger = Logger.getLogger("BM25Algorithm");

    //To perform all the functionalities required in order to generate results
    private static void generateResults(){
        try {
            OpenFile();
            CalculateAVDL();
            QueryProcessing();
            FindTop();
            int qID = 1;
            for (String q : finalScore.keySet()){
                LinkedHashMap<Integer, Double> docIdScore = finalScore.get(q);
                int rank = 1;
                for(Integer docID : docIdScore.keySet()){
                    System.out.println(qID + " " + ARBITRARY_QNAME_CONSTANT + 
                            " " + docID + " " + rank + " " + docIdScore.get(docID)
                            + " " + SYS_NAME_CONSTANT);
                    rank++;
                }
                qID++;
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Exception occurred.", ex);
        }
    }
    
    //To open the indexer file and fill up wordList and docCount
    private static void OpenFile() throws IOException{

        FileInputStream inputStream = new FileInputStream(indexFilePath);
        BufferedReader textReader = new BufferedReader(new InputStreamReader(inputStream));
        String aLine;

        String[] idFrequencyPairs;
        String[] docIdTermFrequency;
        String[] docIdLengthPair;

        while (!(aLine = textReader.readLine()).equals("---")){
            idFrequencyPairs = aLine.split(" ");
            LinkedHashMap<Integer, Integer> docList = new LinkedHashMap();
            //Startinf from 1 as first member would be a query
            for(int i = 1; i < idFrequencyPairs.length; i++){
                docIdTermFrequency = idFrequencyPairs[i].split("=");
                docList.put(Integer.parseInt(docIdTermFrequency[0]),
                        Integer.parseInt(docIdTermFrequency[1]));
            }
            wordList.put(idFrequencyPairs[0], docList);
        }
        while ((aLine = textReader.readLine()) != null){
            docIdLengthPair = aLine.split("-->");
            docCount.put(Integer.parseInt(docIdLengthPair[0]),
                    Integer.parseInt(docIdLengthPair[1]));
        }
    }
        
    private static void CalculateAVDL(){
        double doc=0, length=0;
        for(Integer d : docCount.keySet()){
            doc++;
            length += docCount.get(d);
        }
        avdl = length / doc;
    }

    //To find BM25 score for each query term
    private static void QueryProcessing() throws IOException{
        double k1 = 1.2, b = 0.75, k2 = 100, K, ri=0, R=0, bm25, fi, ni, qfi=1;
        int N = docCount.size();

        FileInputStream inputStream = new FileInputStream(qryFilePath);
        BufferedReader textReader = new BufferedReader(new InputStreamReader(inputStream));
        String aLine;

        String[] query;
        while ((aLine = textReader.readLine()) != null){
            query = aLine.split(" ");
            LinkedHashMap<Integer, Double> bm25Score = new LinkedHashMap();
            for(int i = 1; i <= N; i++){
                bm25 = 0;
                K = (k1 * ((1-b) + (b * docCount.get(i) / avdl)));
                for(int j = 0; j < query.length; j++){
                    if(wordList.get(query[j]).containsKey(i)){
                        fi = wordList.get(query[j]).get(i);
                    }
                    else{
                        fi =0;
                    }
                    ni = wordList.get(query[j]).size();
                    bm25 += Math.log(((ri + 0.5) / (R-ri+0.5)) / 
                                     ((ni-ri+0.5)/(N-ni-R+ri+0.5))) *
                                     (((k1+1) * fi) / (K+fi)) *
                                     (((k2+1) * qfi) / (k2 + qfi));
                }
                bm25Score.put(i,bm25);
            }
            finalScore.put(aLine,bm25Score);
        }
    }

    //To sort out the documents based on BM25 score
    //It will return fixed number of (eg. 100) Documents for each query
    private static void FindTop(){
        LinkedHashMap<Integer, Double> top;
        for(String ls : finalScore.keySet()){
            top = finalScore.get(ls);

            Set<Entry<Integer, Double>> set;
            set = top.entrySet();
            List<Entry<Integer, Double>> sorted = new ArrayList(set);

            Collections.sort(sorted, new Comparator<Entry<Integer, Double>>() {
                @Override
                public int compare(Entry<Integer, Double> o1, Entry<Integer, Double> o2){
                    return (o2.getValue()).compareTo(o1.getValue());
                }
            });
            int i = 1;
            top.clear();
            for (Entry<Integer, Double> entry : sorted){
                top.put(entry.getKey(), entry.getValue());
                if(i == docLimit){
                    break;
                }
                i++;
            }
            finalScore.put(ls, top);
        }
    }
    
    public static void main(String[] args){
        if(args==null || args.length<3){
            System.out.println("Invalid number of arguments. Please refer README for help.");
            System.exit(1);
        }
        indexFilePath = args[0];
        qryFilePath = args[1];
        docLimit = Integer.parseInt(args[2]);
        generateResults();
    }
}