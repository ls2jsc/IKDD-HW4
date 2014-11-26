import java.io.*;
import java.util.*;

public class IKDDhw4 {
	private static final int pageNum = 5;
	
	public static void main(String[] args) throws IOException {	
		/*======================================================*/
		/* searching for keyword and handle dead end.   
		 * using map as adjacent list.
		 * -----------------------------------------
		 * pageMap: adjacent list.     
		 * originMap: adjacent list.
		 * stack: store the dead end page.
		 * keyPage: store keyword page.
		 * cBefore, CNow: calculate weather stop the recursive calculation of finding dead end.
		 */
		Map<Integer, List> pageMap = new HashMap<Integer, List>();
		Map<Integer, List> originMap = new HashMap<Integer, List>();
		Stack<Integer> stack = new Stack<Integer>();
		Stack<Integer> keyPage = new Stack<Integer>();
		String httpS = "http://page";
		int cBefore = pageNum, cNow = pageNum;
		
		for(int i = 1; i <= pageNum; i++) {
			//put all page as nodes.
			List<Integer> pageList = new LinkedList<Integer>();
			List<Integer> originList = new LinkedList<Integer>();
			FileReader fr = new FileReader("page" + i + ".txt");
			BufferedReader br = new BufferedReader(fr);
			//read line by line.
			while (br.ready()) {
				String tmpS = br.readLine(); 
				char httpC[] = tmpS.toCharArray();
				//store pages where keyword in.
				if(tmpS.indexOf(args[0]) != -1 && keyPage.search(i) == -1) {
					keyPage.push(i);
				}
				//build adjacent list.
				if(tmpS.indexOf(httpS) != -1) {
					char tmpC = httpC[tmpS.indexOf(httpS) + httpS.length()];
					int tmpI = tmpC - 48;
					pageList.add(tmpI);
					originList.add(tmpI);
				}
			}
			//store in map.
			pageMap.put(i, pageList);
			originMap.put(i, originList);
		}
		//search dead end and remove it.
		while(true) {
			for(int i = 1; i <= pageNum; i++) {
				List<Integer> pageList = new LinkedList<Integer>();
				pageList = (LinkedList)pageMap.get(i);
				//find dead end.
				if(pageList.isEmpty()) {
					for(int j = 1; j <= pageNum; j++) {
						LinkedList tmpList = new LinkedList();
						tmpList = (LinkedList)pageMap.get(j);
						Integer tmp = new Integer(i);
						if(tmpList.contains(tmp)) {
							//store pages which is dead end.
							if(stack.search(tmp) == -1) stack.push(tmp);
							//remove dead end.
							tmpList.remove(tmp);
							pageMap.put(j, tmpList);
							cNow--;
						}
					}
				}
			}
			if(cBefore == cNow) break;
			else cBefore = cNow;
		}
		//remove dead end.
		for(int i = 1; i <= pageNum; i++) {
			LinkedList tmpList = new LinkedList();
			tmpList = (LinkedList)pageMap.get(i);
			if(tmpList.isEmpty()) pageMap.remove(i);
		}
		/*======================================================*/
		/* build transition matrix.
		 * -------------------------
		 * mat: transition matrix.
		 * voteCount: the number of which other pages link this page.
		 * mati, matj: used for calculation.
		 */
		double[][] mat = new double[pageMap.size()][pageMap.size()];
		double voteCount = 0;
		int mati = 0, matj = 0;
		
		for(Object x : pageMap.keySet()) {
			LinkedList tmpList = new LinkedList();
			tmpList = (LinkedList)pageMap.get(x);
			//calculate the number of other pages link this page.
			for(Object y : pageMap.keySet()) {
				if(tmpList.contains(y)) voteCount++;
			}
			//calculate the probability of each page.
			for(Object y : pageMap.keySet()) {
				if(tmpList.contains(y)) mat[mati][matj] = 1/voteCount;
				mati++;
			}
			voteCount = 0;
			mati = 0;
			matj++;
		}
		/*======================================================*/
		/* Markov process.
		 * --------------
		 * N: the number of pages without dead end.
		 * diff: to ensure previous vector is the same as current one
		 * vector: transition vector.
		 * result: store next transition vector.
		 */
		int N = matj, diff = 0;
		double[] vector = new double[N];
		double[] result = new double[N];
		voteCount = (double)pageMap.size();
		//initialize the vector.
		for(int i = 0; i < N; i++) {
			vector[i] = 1/voteCount;
			result[i] = 0;
		}
		//calculate recursively.
		while(true) {
			for(int i = 0; i < N; i++) {
				for(int j = 0; j < N ;j++) {
					result[i] += mat[i][j] * vector[j];
				}
			}
			for(int i = 0; i < N; i++) {
				if((vector[i] == result[i]))
					diff++;
			}
			//if previous vector is same as current vector, break.
			if(diff == N) break;
			//else, continue calculating transition vector.
			else {
				diff = 0;
				for(int i = 0; i < N; i++) {
					vector[i] = result[i];
					result[i] = 0;
				}
			}
		}
		/*======================================================*/
		/* calculate page rank.
		 * ---------------------
		 * pageRank: store each pages of page rank.
		 */
		double[] pageRank = new double[pageNum];
		int tmpC = 0;
		//initialize the page rank.
		for(int i = 0; i < pageNum; i++){
			pageRank[i] = 0;
		}
		//first, store page rank whose page is without dead end.
		for(Object i : pageMap.keySet()) {
			pageRank[(int)i-1] = result[tmpC];
			tmpC++;
		}
		//next, calculate page rank whose page is dead end.
		while(!stack.empty()) {
			double rank = 0;
			int pop = stack.pop();
			for(Object i : originMap.keySet()) {
				List<Integer> pageList = new LinkedList<Integer>();
				pageList = (LinkedList)originMap.get(i);
				if(pageList.contains(pop)) {
					rank += pageRank[(int)i - 1] / pageList.size();
				}
	        }
			pageRank[pop - 1] = rank;
		}
		/*======================================================*/
		/*ranking.
		 * --------
		 * score: store page rank in this map.
		 * sortList: to sort the rank and store the result.
		 */
		Map score = new HashMap();
		
		while(!keyPage.empty()) {
			int tmpPage = keyPage.pop();
			score.put(tmpPage, pageRank[tmpPage - 1]);
		}
		//sort the page rank.
		List<Map.Entry<Integer, Double>> sortList
		= new ArrayList<Map.Entry<Integer, Double>>(score.entrySet());
		Collections.sort(sortList, new Comparator<Map.Entry<Integer, Double>>()  
		{    
			public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2)  
			{  
				if ((o2.getValue() - o1.getValue()) > 0) return 1;
				else if((o2.getValue() - o1.getValue()) == 0) return 0;
				else return -1;  
			}
		});
		/*======================================================*/
		/*print the result*/
		for(int i = 0; i < sortList.size(); i++) {
			System.out.print((i+1) + "\t\t");
			System.out.println("Page" + sortList.get(i).getKey() + ".txt");
		}
	}
}
