import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.StringTokenizer;

/**
* java AprioriRareMining input output support
* java AprioriRareMining chess.dat outputRareMiing.txt 0.2
*
* 1. Read transactions and all itemsets from file
* 2. Generate all 1-itemsets itemsets
* 3. Calculate all frequent to get a list of Candidat
* 4. Compare itemsets support with minsup. If <, save it as rare itemsets.
* 5. Repeat operations for k-itemsets (k = maximum size of itemsets)
*
*
**/

public class AprioriRareMining extends Observable {

  private List<int[]> itemsets;

  private int numberTransaction;
  private int numberItemSets;
  private int numberRareItemSets;
  private int numberFrequentSets;

  private double minSupRelative;

  private String filename;
  private String fileOutput = null;

  private int k = 0;

  private BufferedWriter writer = null;

  private long timestampBegin = 0L;
  private long timestampEnd = 0L;
  private long memoryBefore = 0L;
  private long memoryAfter = 0L;

  public static void main(String[] args) throws Exception {
    //launch algo
    AprioriRareMining apRare = new AprioriRareMining(args);
  }

  public AprioriRareMining(String[] args) throws Exception {
    timestampBegin = System.currentTimeMillis();
    memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    configure(args);
    explore();
  }

  private void explore() throws Exception{
    generateItemsetsSize1();
    numberFrequentSets = 0;
    numberRareItemSets = 0;
    k = 1;

    //Loop on itemsets (candidats found) and increments k by 1
    while(itemsets.size() > 0){
      calculateFrequentItemsets();
      if(itemsets.size() > 0){
        //increment counter
        numberFrequentSets += itemsets.size();
        generateItemsetsSizeK();
      }
      k++;
    }
    //displayResult
    printResult();
  }

  private void generateItemsetsSizeK(){
    HashMap<String, int[]> tempCandidates = new HashMap<String, int[]>(); //temporary candidates

    for(int i=0 ; i < itemsets.size() ; ++i){
      for(int j = i+i ; j <itemsets.size() ; ++j){
        int[] x = itemsets.get(i);
        int[] y = itemsets.get(j);
        if (x.length != y.length) throw new AssertionError();

        int[] newCandidat = new int[k+1];
        for(int c=0 ; c<newCandidat.length-1 ; ++c){
          newCandidat[c] = x[c];
        }

        int ndifferent = 0;
        for(int s1=0; s1<y.length; s1++)
        {
          boolean found = false;
          // is Y[s1] in X?
            for(int s2=0; s2<x.length; s2++) {
              if (x[s2]==y[s1]) {
                found = true;
                break;
              }
          }
          if (!found){ // Y[s1] is not in X
            ndifferent++;
            // we put the missing value at the end of newCand
            newCandidat[newCandidat.length -1] = y[s1];
          }
        }
        assert(ndifferent >0);

        if(ndifferent == 1){
          Arrays.sort(newCandidat);
          tempCandidates.put(Arrays.toString(newCandidat), newCandidat);
        }
      }

    }
    itemsets = new ArrayList<int[]>(tempCandidates.values());
    log("Created "+itemsets.size()+" unique itemsets of size "+(k+1));
  }

  private void calculateFrequentItemsets() throws Exception{
    log("Passing through the data to determine the frequency of " + itemsets.size()+ " itemsets of size "+k);


    List<int[]> frequentCandidates = new ArrayList<int[]>();
    int count[] = new int[itemsets.size()];
    boolean[] trans = new boolean[numberItemSets];

    boolean match = false;

    BufferedReader data_in = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
    //for each transactions
    for(int i = 0; i < numberTransaction ; ++i){
      String file = data_in.readLine();
      convertFileToBoolArray(file, trans);

      //for each itemset candidate (initialy 1-itemsets)
      for(int j = 0 ; j < itemsets.size() ; ++j){
        int[] candidats = itemsets.get(j);

        for(int c : candidats){
          if(trans[c] == false){
            match = false;
            break;
          }else{
            match = true;
            continue;
          }
        }
        if(match){
          count[j]++;
        }
      }
    }

    //on close le fichier
    data_in.close();

    /**
    * Here, on gonna test each candidat itemsets support (occurence in db)
    * with the minimal support setted.
    * If it's > then it'll be used to generate k+1 -itemsets
    * else, we assume that it is a rare itemsets
    *
    **/

    for(int i = 0 ; i < itemsets.size() ; ++i){
      double supportItemset = (count[i] / (double) numberTransaction);
      if(supportItemset >= minSupRelative){
				frequentCandidates.add(itemsets.get(i));
      } else{
        writeRareFoundToFile(itemsets.get(i), supportItemset);
      }
    }

    itemsets.clear();
    itemsets = frequentCandidates;
  }

  private void convertFileToBoolArray(String file, boolean[] trans) {
    //default value is False
    Arrays.fill(trans, false);
    StringTokenizer tokenized = new StringTokenizer(file, " ");
    while(tokenized.hasMoreTokens()){
      int item = Integer.parseInt(tokenized.nextToken());
      trans[item] = true;
    }
  }

  private void generateItemsetsSize1(){
    itemsets = new ArrayList<int[]>();
      for(int i=0; i<numberItemSets; i++)
      {
      	int[] cand = {i};
      	itemsets.add(cand);
      }
  }

  private void configure(String[] args) throws Exception {
    //settingup all params
    if(args.length != 0) {
      filename = args[0];
    } else {
      //default value
      filename = "chess.dat";
    }

    if(args.length >= 2) {
      fileOutput = args[1];
      this.writer = new BufferedWriter(new FileWriter(fileOutput));
    }

    if(args.length >= 3) {
      minSupRelative = (Double.valueOf(args[2]).doubleValue());
      if(minSupRelative > 1 || minSupRelative < 0) throw new Exception ("[+] error : bad value for minSupRelative line 43");
    } else {
      minSupRelative = .2;
    }

    //read database file and settings counters
    numberTransaction = 0;
    numberItemSets = 0;
    BufferedReader database = new BufferedReader(new FileReader(filename));
    while(database.ready()){
      String line = database.readLine();
      if(line.matches("\\s*")) continue; //matches whitespace
      numberTransaction++;
      //we now use a tokenizert to break the hole line into multiple ones
      StringTokenizer tokenized = new StringTokenizer(line, " ");
      while(tokenized.hasMoreTokens()){
        int x = Integer.parseInt(tokenized.nextToken());
        //log(String.valueOf(x));
        if(x+1 > numberItemSets) numberItemSets = x+1;
      }
    }

    //printing configuration
    printConfig();
  }

  private void printConfig(){
    log("[+] Input configuration: "+numberItemSets+" items, "+numberTransaction+" transactions, ");
    log("-- minsup = "+minSupRelative+"%");
  }

  private void printResult() throws Exception{
    this.timestampEnd = System.currentTimeMillis();
    this.memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

    this.writer.write("=============  APRIORI-RARE - STATS =============");
    this.writer.newLine();
    this.writer.write(" Candidates count : " + this.numberFrequentSets);
    this.writer.newLine();
    this.writer.write(" The algorithm stopped at size " + (this.k - 1));
    this.writer.newLine();
    this.writer.write(" Minimal rare itemsets count : " + this.numberRareItemSets);
    this.writer.newLine();
    this.writer.write("Execution time is: "+((double)(timestampEnd-timestampBegin)/1000) + " seconds.");
    this.writer.newLine();
    this.writer.write("Memory usage is: "+((long)(memoryAfter-memoryBefore)) + " seconds.");
    this.writer.newLine();
    this.writer.write("===================================================");
    this.writer.newLine();
  }

  private void writeRareFoundToFile(int[] itemset, double support) throws Exception{
    this.numberRareItemSets++;
    this.writer.write(Arrays.toString(itemset) + "  ("+support+")");
    this.writer.newLine();
  }

  private void log(String message) {
    System.out.println(message);
  }
}
