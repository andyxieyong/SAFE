package lu.uni.svv.StressTesting;

import junit.framework.TestCase;
import lu.uni.svv.StressTesting.datatype.FitnessList;
import lu.uni.svv.StressTesting.search.model.StatisticalTest;
import lu.uni.svv.StressTesting.search.model.TimeListSolution;
import lu.uni.svv.StressTesting.utils.ArgumentParser;
import org.apache.commons.math3.genetics.Fitness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CompareTest extends TestCase {
	
	public CompareTest(String testName )
	{
		super( testName );
	}

//	public void testCompare1()
//	{
//		System.out.println("\nCompare1: int different");
//
//		FitnessList list1 = new FitnessList();
//		FitnessList list2 = new FitnessList();
//
//		list1.add(new BigDecimal("3"));
//		list1.add(new BigDecimal("4"));
//		list1.add(new BigDecimal("2"));
//		list1.add(new BigDecimal("6"));
//		list1.add(new BigDecimal("2"));
//		list1.add(new BigDecimal("5"));
//
//		list2.add(new BigDecimal("9"));
//		list2.add(new BigDecimal("7"));
//		list2.add(new BigDecimal("5"));
//		list2.add(new BigDecimal("10"));
//		list2.add(new BigDecimal("6"));
//		list2.add(new BigDecimal("8"));
//
//		System.out.println("result="+compare(list1, list2));
//	}
//
//	public void testCompare2() {
//		System.out.println("\nCompare2: same int");
//		FitnessList list1 = new FitnessList();
//		FitnessList list2 = new FitnessList();
//
//		list1.add(new BigDecimal("3"));
//		list1.add(new BigDecimal("4"));
//		list1.add(new BigDecimal("2"));
//		list1.add(new BigDecimal("6"));
//		list1.add(new BigDecimal("2"));
//		list1.add(new BigDecimal("5"));
//
//		list2.add(new BigDecimal("3"));
//		list2.add(new BigDecimal("4"));
//		list2.add(new BigDecimal("2"));
//		list2.add(new BigDecimal("6"));
//		list2.add(new BigDecimal("2"));
//		list2.add(new BigDecimal("5"));
//
//		System.out.println("result="+compare(list1, list2));
//	}
//	public void testCompare3() {
//		System.out.println("\nCompare3: float");
//		FitnessList list1 = new FitnessList();
//		FitnessList list2 = new FitnessList();
//
//		list1.add(new BigDecimal("37.23"));
//		list1.add(new BigDecimal("42.45"));
//		list1.add(new BigDecimal("47.75"));
//		list1.add(new BigDecimal("51.24"));
//		list1.add(new BigDecimal("51.58"));
//		list1.add(new BigDecimal("52.67"));
//		list1.add(new BigDecimal("53.64"));
//		list1.add(new BigDecimal("56.56"));
//		list1.add(new BigDecimal("56.54"));
//		list1.add(new BigDecimal("63.43"));
//		list1.add(new BigDecimal("97.68"));
//		list1.add(new BigDecimal("100.59"));
//
//
//
//		list2.add(new BigDecimal("47.12"));
//		list2.add(new BigDecimal("47.72"));
//		list2.add(new BigDecimal("58.13"));
//		list2.add(new BigDecimal("53.54"));
//		list2.add(new BigDecimal("57.84"));
//		list2.add(new BigDecimal("61.50"));
//		list2.add(new BigDecimal("69.78"));
//		list2.add(new BigDecimal("70.73"));
//		list2.add(new BigDecimal("104.27"));
//		list2.add(new BigDecimal("110.56"));
//		list2.add(new BigDecimal("132.82"));
//		list2.add(new BigDecimal("120.50"));
//
//
//		System.out.println("result="+compare(list1, list2));
//	}
//
//	public void testCompare5() {
//		System.out.println("\nCompare5: float");
//		FitnessList list1 = new FitnessList();
//		FitnessList list2 = new FitnessList();
//
//		list1.add(new BigDecimal("37.23"));
//		list1.add(new BigDecimal("42.45"));
//		list1.add(new BigDecimal("47.75"));
//		list1.add(new BigDecimal("51.24"));
//		list1.add(new BigDecimal("51.58"));
//		list1.add(new BigDecimal("52.67"));
//		list1.add(new BigDecimal("53.64"));
//		list1.add(new BigDecimal("56.56"));
//		list1.add(new BigDecimal("56.54"));
//		list1.add(new BigDecimal("63.43"));
//		list1.add(new BigDecimal("97.68"));
//		list1.add(new BigDecimal("100.59"));
//
//
//
//		list2.add(new BigDecimal("63.12"));
//		list2.add(new BigDecimal("47.72"));
//		list2.add(new BigDecimal("56.13"));
//		list2.add(new BigDecimal("53.54"));
//		list2.add(new BigDecimal("57.84"));
//		list2.add(new BigDecimal("61.50"));
//		list2.add(new BigDecimal("99.78"));
//		list2.add(new BigDecimal("70.73"));
//		list2.add(new BigDecimal("104.27"));
//		list2.add(new BigDecimal("110.56"));
//		list2.add(new BigDecimal("132.82"));
//		list2.add(new BigDecimal("120.50"));
//
//
//		System.out.println("result="+compare(list1, list2));
//	}
//
//
//	public void testCompare4() {
//		System.out.println("\nCompare4: BigDecimal");
//		FitnessList list1 = new FitnessList();
//		FitnessList list2 = new FitnessList();
//
//		list1.add(new BigDecimal("2.613863214149480056211444502E-2918"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-2918"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-2918"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-2918"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-2918"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-2918"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-2918"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-2918"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-2918"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-2918"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-2918"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-2918"));
//
//		list2.add(new BigDecimal("2.613863214149480056211444502E-1118"));
//		list2.add(new BigDecimal("2.613863214149480056211444502E-2918"));
//		list2.add(new BigDecimal("2.613863214149480056211444502E-2918"));
//		list2.add(new BigDecimal("2.613863214149480056211444502E-2918"));
//		list2.add(new BigDecimal("2.613863214149480056211444502E-2918"));
//		list2.add(new BigDecimal("2.613863214149480056211444502E-2918"));
//		list2.add(new BigDecimal("2.613863214149480056211444502E-2918"));
//		list2.add(new BigDecimal("2.613863214149480056211444502E-2918"));
//		list2.add(new BigDecimal("2.613863214149480056211444502E-2918"));
//		list2.add(new BigDecimal("2.613863214149480056211444502E-2918"));
//		list2.add(new BigDecimal("2.613863214149480056211444502E-2918"));
//		list2.add(new BigDecimal("2.613863214149480056211444502E-2918"));
//
//		System.out.println("result="+compare(list1, list2));
//
//	}
//
//
//
//	public void testCompare6() {
//		System.out.println("\nCompare6: BigDecimal");
//		FitnessList list1 = new FitnessList();
//		FitnessList list2 = new FitnessList();
//
//		list1.add(new BigDecimal("2.613863214149480056211444502E-291"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-291"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-291"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-291"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-291"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-291"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-291"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-291"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-291"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-291"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-291"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-291"));
//
//		list2.add(new BigDecimal("2.613863214149480056211444502E-11"));
//		list2.add(new BigDecimal("2.613863214149480056211444502E-21"));
//		list2.add(new BigDecimal("2.613863214149480056211444502E-298"));
//		list2.add(new BigDecimal("2.613863214149480056211444502E-91"));
//		list2.add(new BigDecimal("2.613863214149480056211444502E-91"));
//		list2.add(new BigDecimal("2.613863214149480056211444502E-291"));
//		list2.add(new BigDecimal("2.613863214149480056211444502E-291"));
//		list2.add(new BigDecimal("2.613863214149480056211444502E-21"));
//		list2.add(new BigDecimal("2.613863214149480056211444502E-28"));
//		list2.add(new BigDecimal("2.613863214149480056211444502E-151"));
//		list2.add(new BigDecimal("2.613863214149480056211444502E-291"));
//		list2.add(new BigDecimal("2.613863214149480056211444502E-51"));
//
//		System.out.println("result="+compare(list1, list2));
//
//	}
//
//
//	public void testCompare7() {
//		System.out.println("\nCompare7: BigDecimal");
//		FitnessList list1 = new FitnessList();
//		FitnessList list2 = new FitnessList();
//
//		list1.add(new BigDecimal("2.613863214149480056211444502E-291"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-291"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-291"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-291"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-291"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-291"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-291"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-291"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-291"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-291"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-291"));
//		list1.add(new BigDecimal("2.613863214149480056211444502E-291"));
//
//		list2.add(new BigDecimal("2.613863214149980056211944502E-291"));
//		list2.add(new BigDecimal("2.613863214149980056211944502E-291"));
//		list2.add(new BigDecimal("2.613863214149980056211944502E-291"));
//		list2.add(new BigDecimal("2.613863214149980056211944502E-291"));
//		list2.add(new BigDecimal("2.613863214149980056211944502E-291"));
//		list2.add(new BigDecimal("2.613863214149980056211944502E-291"));
//		list2.add(new BigDecimal("2.613863214149980056211944502E-291"));
//		list2.add(new BigDecimal("2.613863214149980056211944502E-291"));
//		list2.add(new BigDecimal("2.613863214149980056211944502E-291"));
//		list2.add(new BigDecimal("2.613863214149980056211944502E-291"));
//		list2.add(new BigDecimal("2.613863214149980056211944502E-291"));
//		list2.add(new BigDecimal("2.613863214149980056211944502E-291"));
//
//		System.out.println("result="+compare(list1, list2));
//
//	}
//
	
	public int compare(FitnessList list1, FitnessList list2){
	
		boolean diff = StatisticalTest.UtestSignificance(list1, list2);
		System.out.println("diff=" + diff);
		if (diff == true) {
			int result = 0;
			double value = 0.0;
			value = StatisticalTest.A12(list1, list2);
			if (value > 0.5) result = 1;
			else if (value < 0.5) result = -1;
			
			System.out.println("value=" + value);
			return result;
		}
		else{
			return 0;
		}
		
	}
	
}
