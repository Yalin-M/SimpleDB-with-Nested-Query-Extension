package simpledb.planner;

import simpledb.tx.Transaction;
import simpledb.query.*;
import simpledb.parse.*;
import simpledb.server.SimpleDB;

import java.util.*;

/**
 * The simplest, most naive query planner possible.
 * @author Edward Sciore
 */
public class BasicQueryPlanner implements QueryPlanner {
   
	private HashMap<Integer, QueryData> queryMap = new HashMap<Integer, QueryData>();
	private List<Plan> plans = new ArrayList<Plan>();
   /**
    * Creates a query plan as follows.  It first takes
    * the product of all tables and views; it then selects on the predicate;
    * and finally it projects on the field list. 
    */
   public Plan createPlan(QueryData data, Transaction tx, Parser parser) {
	  
	  this.queryMap = parser.getQueryMaps();
      
	  for (String tblname : data.tables()) {
         String viewdef = SimpleDB.mdMgr().getViewDef(tblname, tx);
         if (viewdef != null)
            plans.add(SimpleDB.planner().createQueryPlan(viewdef, tx));
         else
            plans.add(new TablePlan(tblname, tx));
      }
      
      addNestedQueriesToPlan(tx);
      
      if(queryMap.size() > 0){
    	  int size = plans.size();
    	  Plan nestedPlans = nestedQueryPlans(queryMap.size() -1, plans.get(size-1));
    	  Plan firstPlan = plans.remove(0);
    	  firstPlan = new ProductPlan(firstPlan, nestedPlans);
    	  firstPlan = new SelectPlan(firstPlan, data.pred());
    	  firstPlan = new ProjectPlan(firstPlan, data.fields());
    	  return firstPlan;
      }
      
      Plan p = plans.remove(0);
      for (Plan nextplan : plans){
         p = new ProductPlan(p, nextplan);
      }
      
      p = new SelectPlan(p, data.pred());
      p = new ProjectPlan(p, data.fields());
      return p;
   }
   
   private void addNestedQueriesToPlan(Transaction tx){
	   for(int i = 0; i < queryMap.size(); i++){
	    	  for(String tblname : queryMap.get(i).tables()){
	    		  String viewdef = SimpleDB.mdMgr().getViewDef(tblname, tx);
	    	         if (viewdef != null)
	    	            plans.add(SimpleDB.planner().createQueryPlan(viewdef, tx));
	    	         else
	    	            plans.add(new TablePlan(tblname, tx));
	    	  }
	      }
   }
   
   private Plan nestedQueryPlans(int size, Plan p){
	   if(size < 0){
		   return p;
	   } else {
		  p = new SelectPlan(p, queryMap.get(size).pred());
		  p = new ProjectPlan(p, queryMap.get(size).fields());
		  return nestedQueryPlans(--size, p);
	   }
   }
}
