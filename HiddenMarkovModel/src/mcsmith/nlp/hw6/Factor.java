package mcsmith.nlp.hw6;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class Factor {
	
	public class FactorException extends Exception
    {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public FactorException () {super();}
    	public FactorException (String message) {
        	super (message);
        }
    	public FactorException (Throwable cause) {
        	super (cause);
        }
    	public FactorException (String message, Throwable cause) {
    		super (message, cause);
        }
    }
	public class FactorIndexException extends FactorException {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public FactorIndexException () {super();}
    	public FactorIndexException (String message) {
        	super (message);
        }
    	public FactorIndexException (Throwable cause) {
        	super (cause);
        }
    	public FactorIndexException (String message, Throwable cause) {
    		super (message, cause);
        }
	}
	public class FactorScopeException extends FactorException {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public FactorScopeException () {super();}
    	public FactorScopeException (String message) {
        	super (message);
        }
    	public FactorScopeException (Throwable cause) {
        	super (cause);
        }
    	public FactorScopeException (String message, Throwable cause) {
    		super (message, cause);
        }
	}
	
	public class Pair<A, B> {
	    public A first;
	    public B second;

	    public Pair(A first, B second) {
	    	super();
	    	this.first = first;
	    	this.second = second;
	    }
	}
	
	protected static ArrayList<String> _variableNames;
	protected static ArrayList<ArrayList<String>> _variableValues;
	protected static ArrayList<Integer> _variableCard;
	
	public static void addVariable(String varName, ArrayList<String> varValues){
		if(_variableNames == null) _variableNames = new ArrayList<String>();
		if(_variableValues == null) _variableValues = new ArrayList<ArrayList<String>>();
		if(_variableCard == null) _variableCard = new ArrayList<Integer>();
		
		_variableNames.add(varName);
		_variableValues.add(varValues);
		_variableCard.add(varValues.size());
//		System.out.printf("var:%s values:%s card:%s\n", varName, varValues, 
//				varValues.size());
	}
	
	
	public String getVariableNames(){
		String names = "";
		for(int i:_variables){
			names += _variableNames.get(i);
		}
		return names;
	}
	public static int getVariableIndex(String var){
		if(_variableNames == null)
			return -1;
		return _variableNames.indexOf(var);
	}
	public static String getVariableName(int i){
		if(_variableNames == null)
			return "";
		return _variableNames.get(i);
	}
	public static ArrayList<Integer> variableNamesToIndicies(ArrayList<String> variables){
		ArrayList<Integer> indicies = new ArrayList<Integer>();
		for(String s:variables){
			indicies.add(_variableNames.indexOf(s));
		}
		return indicies;
	}
	public static ArrayList<Integer> variableNamesToIndicies(String[] variables){
		ArrayList<Integer> indicies = new ArrayList<Integer>();
		for(String s:variables){
			indicies.add(_variableNames.indexOf(s));
		}
		return indicies;
	}
	public static ArrayList<String> variableIndicesToNames(ArrayList<Integer> variables){
		ArrayList<String> names = new ArrayList<String>();
		for(int s:variables){
			names.add(_variableNames.get(s));
		}
		return names;
	}
	public static ArrayList<String> variableIndicesToNames(int... variables){
		ArrayList<String> names = new ArrayList<String>();
		for(int s:variables){
			names.add(_variableNames.get(s));
		}
		return names;
	}
	
	public static int getVariableValueIndex(int varIdx, String val) throws ArrayIndexOutOfBoundsException {
		if(_variableValues == null)
			return -1;
		return _variableValues.get(varIdx).indexOf(val);
	}
	public static String getVariableName(int varIdx, int valueIdx){
		if(_variableNames == null)
			return "";
		return _variableValues.get(varIdx).get(valueIdx);
	}
	public static ArrayList<Integer> valueNamesToIndicies(ArrayList<String> variables, ArrayList<String> var_value){
		ArrayList<Integer> indicies = new ArrayList<Integer>();
		
		for(int i = 0; i < variables.size(); i++){
			indicies.add(_variableValues.get(getVariableIndex(variables.get(i))).indexOf(var_value.get(i)));
		}
		return indicies;
	}
	public static ArrayList<String> valueIndiciesToNames(ArrayList<Integer> variables, ArrayList<Integer> var_value){
		ArrayList<String> valueNames = new ArrayList<String>();
		
		for(int i = 0; i < variables.size(); i++){
			valueNames.add(_variableValues.get(variables.get(i)).get(var_value.get(i)));
		}
		return valueNames;
	}
	public static String valueIndexToName(int variable, int var_value){
//		ArrayList<String> valueNames = new ArrayList<String>();
		return _variableValues.get(variable).get(var_value);
	}
	
	public static String variableInfo(){
		String output = "";
		if(_variableNames == null){
			output += "_variableNames is null.\n";
		}
		if(_variableValues == null){
			output += "_variableValues is null.\n";
		}
		if(_variableCard == null){
			output += "_variableCard is null.\n";
		}
		if(_variableNames != null && _variableValues != null && _variableCard != null ){
			output+= "_variableNames size: " + _variableNames.size() +"\n";
			for(int varIdx = 0; varIdx<_variableNames.size(); varIdx++){
				output+= varIdx + " aka " +_variableNames.get(varIdx) + " size(" + _variableCard.get(varIdx) +")[";
				for(int valIdx = 0; valIdx < _variableValues.get(varIdx).size(); valIdx++){
					output+=" "+_variableValues.get(varIdx).get(valIdx);
				}
				output+=" ]\n";
			}
		}
		
		return output;
	}
	
	protected ArrayList<Integer> lhs,rhs;
	
	protected ArrayList<Integer> _variables;
	protected ArrayList<Integer> _stride;
	
	protected ArrayList<Double> data; //log probabilities
	
	Factor(String[] varsNames){
		_variables= new ArrayList<Integer>(varsNames.length);
		for(int index=0; index < varsNames.length; index ++) 
			_variables.add(_variableNames.indexOf(varsNames[index]));
		
		Collections.sort(_variables);
		
		this._stride = new ArrayList<Integer>(_variables.size());
		int strideTot = 1;
		/*
		 for(int index:_variables){
			_stride.add(strideTot);
			strideTot*=_variableCard.get(index);
		}
		*/
		for(int i = _variables.size()-1 ; i >=0 ; i--){
			_stride.add(0, strideTot);
			strideTot*=_variableCard.get(_variables.get(i));
		}
		
		
		this.data = new ArrayList<Double>(strideTot);
		for(int i = 0; i<strideTot; i++) data.add(Math.log(1.0));
	}
	
	protected Factor(ArrayList<Integer> vars){
		this._variables = vars;
		Collections.sort(_variables);
		this._stride = new ArrayList<Integer>(_variables.size());
		int strideTot = 1;
		/*
		for(int index:_variables){
			_stride.add(strideTot);
			strideTot*=_variableCard.get(index);
		}
		*/
		for(int i = _variables.size()-1 ; i >=0 ; i--){
			_stride.add(0, strideTot);
			strideTot*=_variableCard.get(_variables.get(i));
		}
		
		this.data = new ArrayList<Double>(strideTot);
		for(int i = 0; i<strideTot; i++) data.add(Math.log(1.0));
	}
	
	public Factor(Factor factToCopy){
		this._variables = factToCopy._variables;
		this._stride = factToCopy._stride;
		
		this.data = new ArrayList<Double>(factToCopy.data.size());
		for(int i = 0; i<factToCopy.data.size(); i++) data.add(factToCopy.data.get(i));
	}
	
	public Factor(ArrayList<Integer> vars, double d) {
		this._variables = vars;
		Collections.sort(_variables);
		this._stride = new ArrayList<Integer>(_variables.size());
		int strideTot = 1;
		/*
		for(int index:_variables){
			_stride.add(strideTot);
			strideTot*=_variableCard.get(index);
		}*/
		for(int i = _variables.size()-1 ; i >=0 ; i--){
//			System.out.printf("i:%d var:%d aka %s card:%d adding stride:%d strideTot:%d\n",
//					i, _variables.get(i), Factor.variableIndicesToNames(_variables.get(i)),
//					_variableCard.get(_variables.get(i)),
//					strideTot,
//					strideTot*_variableCard.get(_variables.get(i)) 
//					);
			_stride.add(0, strideTot);
			strideTot*=_variableCard.get(_variables.get(i));
		}
		
		this.data = new ArrayList<Double>(strideTot);
		for(int i = 0; i<strideTot; i++) data.add(Math.log(d));
	}

	public void setFactorData(Factor f) throws FactorScopeException {
		if(this._variables.equals(f._variables)){
			for(int i = 0; i<data.size(); i++)
				this.data.set(i, f.data.get(i));
		}else{
			throw new FactorScopeException("can't see this factor, does not contain the same variables");
		}
	}
	
	private int getInternalIndex(int globalIndex){
		return _variables.indexOf(globalIndex);
	}
	
	interface Callback {
		void iterate(int[] curValue); // n-dimensional point
	}
	void iterate(ArrayList<Integer> heldVariables, ArrayList<Integer> heldValues, int currentDimension, int[] js, Callback c) {
		for (int i = 0; i < _variableCard.get(currentDimension); i++) {
			if(heldVariables.contains(i)){
				js[currentDimension] =  heldValues.get(heldVariables.indexOf(i));
			}else{
				js[currentDimension] =  i;
			}
			
	        if (currentDimension == js.length - 1) c.iterate(js);
	        else iterate(heldVariables,heldValues, currentDimension + 1, js, c);

		}
	}

	private int index(ArrayList<Integer> variables, ArrayList<Integer> values) throws FactorIndexException {
		int[] temp = new int[variables.size()];
		for(int i = 0; i< variables.size();i++){
			if(_variables.indexOf(variables.get(i)) == -1) {
				throw new FactorIndexException("index error: factor does not contain desired variable");
			}
			temp[_variables.indexOf(variables.get(i))] = values.get(i);
		}
		return index(temp);
	}
	private int index(int[] variableValues) throws FactorIndexException{
		int searchIndex = 0;
		
		if(variableValues.length != _variables.size()) 
			throw new FactorIndexException("FactorIndexError: indexLength("+variableValues.length+") does not match number of variables for this factor("+_variables.size()+")");
		
		for(int index=0; index < _variables.size(); index ++){
			if(variableValues[index] >= _variableCard.get(_variables.get(index))|| variableValues[index]< 0)
				throw new FactorIndexException("FactorIndexError: variableValue("+variableValues[index]+") was not in the valid range of ( 0 - "+(_variableCard.get(_variables.get(index))-1)+")");
			else
				searchIndex += variableValues[index]*_stride.get(index);
		}
		
		return searchIndex;
	}
	
	public ArrayList<Integer> valuesFromIndex(int datum_index) throws FactorIndexException {
		
		
		if(datum_index >= data.size()|| datum_index< 0)
			throw new FactorIndexException("FactorIndexError: index("+datum_index+") was not in the valid range of ( 0 - "+(data.size()-1)+")");
		
		ArrayList<Integer> values = new ArrayList<Integer>();
		
		
//		System.out.println("valuesFromIndex");
//		System.out.println("index:" + datum_index);
//		System.out.println("_variables"+_variables );
//		System.out.println("_stride"+_stride );
//		System.out.println("_variableCard"+_variableCard );
//		System.out.println();
		
//		for(int varIdx = _variables.size() -1; varIdx >= 0; varIdx--){
		for(int varIdx = 0; varIdx< _variables.size(); varIdx++){
//			System.out.println("var:%d aka %s\n", varIdx);
//			System.out.printf("(index / stride[i]) mod card[i]\n");
//			System.out.printf("(%d / %d) mod %d\n", datum_index, _stride.get(varIdx), _variableCard
//					.get(_variables.get(varIdx)));
			values.add((datum_index/_stride.get(varIdx))%_variableCard.get(_variables.get(varIdx)));
		}
		
		return values;
	}
	
	public void putProbByName(String[] varVals, double prob) 
			throws FactorIndexException, FactorScopeException {
		if(varVals.length != _variables.size()) 
			throw new FactorIndexException("InputLengthError: indexLength("+varVals.length+") does not match number of variables for this factor("+_variables.size()+")");
		ArrayList<Integer> valVarsIndicies = new ArrayList<Integer>();
		for(int varIdx=0; varIdx < varVals.length; varIdx ++){
			int integerValOfString = _variableValues.get(varIdx).indexOf(varVals[varIdx]);
			if(integerValOfString <0)
				throw new FactorScopeException("ValueError: Value("+varVals[varIdx]+") not applicable for "+_variableNames.get(varIdx));
			valVarsIndicies.add(integerValOfString);
		}
		putProbByValues(valVarsIndicies,prob);
		
	}
	
	public void putProbByIndex(int datum_index, double prob) throws FactorIndexException{
		if(datum_index < 0 || datum_index > data.size()) 
			throw new FactorIndexException("InputLengthError: datumIndex("+datum_index+") is not in range ( 0 - "+_variables.size()+")");
		
		data.set(datum_index,Math.log(prob));
		
	}
	
	public String toString(){
		String output = "";
		if(_variables.size() != 0) {
			// print out a description of the factor
			output += "Phi( ";
			for(int var_idx=0; var_idx < _variables.size(); var_idx ++)
				output += _variableNames.get(_variables.get(var_idx)) + " ";
			output += ")\n";
			
//			for(int var_idx=0; var_idx < _variables.size(); var_idx ++)
//				output += _stride.get(var_idx) + "\t";
//			output += "Stride\n";
			
			for(int var_idx=0; var_idx < _variables.size(); var_idx ++)
				output += _variableNames.get(_variables.get(var_idx)) + "\t";
			output += "Value\n";
		}
		for(int datum_index = 0; datum_index < data.size(); datum_index++){
			
			ArrayList<Integer> values = null;
			try {
				values = valuesFromIndex(datum_index);
//				System.out.printf("data:%d values:%s\n",datum_index, values);
			} catch (FactorIndexException e) {e.printStackTrace();}
			
			/*
			System.out.println(_variables);
			System.out.println(values);
			ArrayList<String> valueNames = valueIndiciesToNames(_variables,values);
			System.out.println(valueNames);
			*/
			for(int var_idx=0; var_idx < _variables.size(); var_idx ++){
//				output += String.format("%01d\t", values.get(var_idx));
				output += String.format("%s\t", Factor.valueIndexToName(var_idx, values.get(var_idx)));
				//output += valueNames.get(var_idx) + "\t";
					//	String.format("%s\t",
					//	.get(var_idx),);
					//Factor.getVariableName(var_idx,
					//		((datum_index/_stride.get(var_idx))%_variableCard.get(var_idx))));
						
			}
			output += Math.exp(data.get(datum_index))+"\n";
		}
		return output.substring(0, output.length()-1);
	}
	
	public void putProbByValues(double prob,int... values )
			throws FactorIndexException {
		
		int searchIndex = 0;
		if(values.length != _variables.size()) 
			throw new FactorIndexException("FactorIndexError: indexLength("+values.length+") does not match number of variables for this factor("+_variables.size()+")");
		for(int index=0; index < _variables.size(); index ++)
			if(values[index] >= _variableCard.get(_variables.get(index))|| values[index]< 0)
				throw new FactorIndexException("FactorIndexError: variableValue("+values[index]+") was not in the valid range of ( 0 - "+(_variableCard.get(_variables.get(index))-1)+")");
			else
				searchIndex += values[index]*_stride.get(index);
		
		data.set(searchIndex,Math.log(prob));
	}
	
	public void putProbByValues(ArrayList<Integer> arrayList,double prob) 
			throws FactorIndexException {
		
		int searchIndex = 0;
		if(arrayList.size() != _variables.size()) 
			throw new FactorIndexException("FactorIndexError: indexLength("+arrayList.size()+") does not match number of variables for this factor("+_variables.size()+")");
		// for every one of our variables...
		for(int index=0; index < _variables.size(); index ++) {
			int variable = _variables.get(index);
			// check if we are given a valid value for that variable.
			if(arrayList.get(index) >= _variableCard.get(variable)
					|| arrayList.get(index)< 0) {
//				System.out.printf("for index:%d variable:%d aka %s\nthought card:%d\nsupplied value:%d\n", 
//					index, variable, Factor.variableIndicesToNames(variable), 
//					_variableCard.get(variable),
//					arrayList.get(index));
				throw new FactorIndexException("FactorIndexError: variableValue("+arrayList.get(index)+") was not in the valid range of ( 0 - "+(_variableCard.get(_variables.get(index))-1)+")");
			}
			else
				searchIndex += arrayList.get(index)*_stride.get(index);
		}
		
		data.set(searchIndex,Math.log(prob));
	}
	
	public double getProbByValues(int[] variableValues) throws FactorIndexException {	
		return data.get(index(variableValues));
	}
	
	public ArrayList<Integer> intersection(ArrayList<Integer> other){
		Set<Integer> myVars = new TreeSet<Integer>(this._variables);
		Set<Integer> theirVars = new TreeSet<Integer>(other);
		
		myVars.retainAll(theirVars);
		ArrayList<Integer> intersection = new ArrayList<Integer>();
		for(Object o:myVars.toArray())
			intersection.add((Integer) o);
		return intersection;
	}
	
	public ArrayList<Integer> difference(ArrayList<Integer> other){
		Set<Integer> myVars = new TreeSet<Integer>(this._variables);
		Set<Integer> theirVars = new TreeSet<Integer>(other);
		
		myVars.removeAll(theirVars);
		ArrayList<Integer> difference = new ArrayList<Integer>();
		
		for(Object o:myVars.toArray())
			difference.add((Integer) o);
		return difference;
	}
	
	public ArrayList<Integer> union(ArrayList<Integer> other){
		Set<Integer> myVars = new TreeSet<Integer>(this._variables);
		Set<Integer> theirVars = new TreeSet<Integer>(other);
		
		myVars.addAll(theirVars);
		ArrayList<Integer> union = new ArrayList<Integer>();
		
		for(Object o:myVars.toArray())
			union.add((Integer) o);
		return union;
	}

	public boolean contains(ArrayList<Integer> other){
			Set<Integer> myVars = new TreeSet<Integer>(this._variables);
			Set<Integer> theirVars = new TreeSet<Integer>(other);
			//System.out.println(myVars + " ?= "+ theirVars);
			return myVars.containsAll(theirVars);
		}
	
	//p359 example problem p107
	public Factor product(Factor f) throws ArrayIndexOutOfBoundsException {
		ArrayList<Integer> unionScope = this.union(f._variables);
		Factor psi = new Factor(unionScope);
		
		int j =0;
		int k =0;
		int[] assigment = new int[unionScope.size()];
		
		for(int i = 0; i < psi.data.size(); i++){
			psi.data.set(i, this.data.get(j)+f.data.get(k)); //adding log probabilies
			//System.out.printf("Multiplying value at (%d) by (%d) to get value at (%d)\n",j,k,i);
			
			//for(int l =0; l < unionScope.size(); l++){
			 for(int l =unionScope.size()-1; l >=0 ; l--){
				assigment[l]++;
				//System.out.printf("l = %d\n",l);
				if(assigment[l]==_variableCard.get(unionScope.get(l))){
					//System.out.printf("assignment[%d] == card(%d)\n",unionScope.get(l),unionScope.get(l));
					assigment[l]=0;
					if(this._variables.contains(unionScope.get(l)))
						j = j-(_variableCard.get(unionScope.get(l))-1)*this._stride.get(this.getInternalIndex(unionScope.get(l)));
					if(f._variables.contains(unionScope.get(l)))
						k = k-(_variableCard.get(unionScope.get(l))-1)*f._stride.get(f.getInternalIndex(unionScope.get(l)));
				}else{
					//System.out.printf("Else\n");
					if(this._variables.contains(unionScope.get(l)))
						j = j + this._stride.get(this.getInternalIndex(unionScope.get(l)));
					if(f._variables.contains(unionScope.get(l)))
						k = k + f._stride.get(f.getInternalIndex(unionScope.get(l)));
					break;
				}
			}
		}
		
		
		return psi;
	}
	
	public Factor divide(Factor f) throws FactorScopeException, FactorIndexException {
		if(!this._variables.containsAll(f._variables))
			throw new FactorScopeException("DivionError: Numerator does not contain Denominator");
		
		Factor result = new Factor(this._variables);
		
		ArrayList<Integer> sepset = this.intersection(f._variables);
		
		//System.out.println("Dividing: "+ this._variables + " by "+ f._variables + ", intersection of: " + sepset  +"("+sepset.size()+")");
		//System.out.println("LHS datasize: "+ this.data.size() + " RHS datasize: " + f.data.size() );
		for(int datum_idx = 0; datum_idx < this.data.size(); datum_idx++ ){
			ArrayList<Integer> values = valuesFromIndex(datum_idx);
			//ArrayList<Integer> sharedVarValues = new ArrayList<Integer>();
			
			int[] f_indicies_of_values = new int[sepset.size()];
			for(int s:sepset){
				int this_ind = this._variables.indexOf(s);//index of variable in sepset in this factor
				//sharedVarValues.add(values[this_ind]);//value of that variable
				int that_ind = f._variables.indexOf(s);//index of variable in sepset in this factor
				f_indicies_of_values[that_ind] = values.get(this_ind);
			}
			if(this.data.get(datum_idx).equals(Double.NEGATIVE_INFINITY) && f.getProbByValues(f_indicies_of_values) == Double.NEGATIVE_INFINITY ){
				result.data.set(datum_idx, Double.NEGATIVE_INFINITY);
			}else{
				result.data.set(datum_idx, this.data.get(datum_idx) - f.getProbByValues(f_indicies_of_values));
			}
			
		}
		
		return result;
	}
	
	//example p297
	public Factor marginalize(ArrayList<Integer> elimVar) throws FactorIndexException{
		ArrayList<Integer> finalVars = difference(elimVar);
//		System.out.println("my vars:"+_variables+" aka "+Factor.variableIndicesToNames(_variables));
//		System.out.println("final vars:"+finalVars+" aka "+Factor.variableIndicesToNames(finalVars));
		Factor result = new Factor(finalVars, 0);
//		System.out.println("\ninital factor of 0's:");
//		System.out.println(result.toString());
//		System.out.println("\nbegin marginalizing");
		
		for(int datum_idx = 0; datum_idx < this.data.size(); datum_idx++ ){
			ArrayList<Integer> values = valuesFromIndex(datum_idx);
			
			int[] f_indicies_of_values = new int[finalVars.size()];
			//ArrayList<Integer> sharedVarValues = new ArrayList<Integer>();
			
			for(int s:finalVars){
				
				int this_ind = this._variables.indexOf(s);//index of variable in sepset in this factor
				//sharedVarValues.add(values[this_ind]);//value of that variable
				int that_ind = result._variables.indexOf(s);//index of variable in sepset in this factor
				f_indicies_of_values[that_ind] = values.get(this_ind);
				
			}
//			System.out.printf("this:%e result:%f\n", Math.exp(this.data.get(datum_idx)), 
//					Math.exp(result.data.get(result.index(f_indicies_of_values))));
			result.data.set(result.index(f_indicies_of_values), 
					Math.log(
							Math.exp(result.data.get(result.index(f_indicies_of_values)))
							+Math.exp(this.data.get(datum_idx))));			
		}
		return result;
	}

	public void normalize(){
		// in prob space
//		double Z = 0;
//		for(int i = 0; i<data.size(); i++){
//			Z += Math.exp(data.get(i));
//		}
//		double logZ = Math.log(Z);
//		for(int i = 0; i<data.size(); i++){
//			data.set(i,data.get(i)-logZ);
//		}
		// log prob space
		double maxLog = Double.NEGATIVE_INFINITY;
		for(int i = 0; i < data.size(); i++) {
			if(data.get(i) > maxLog) maxLog = data.get(i);
		}
		double sum = 0;
		for(int i = 0; i < data.size(); i++) {
			sum += Math.exp(data.get(i) - maxLog);
		}
		double logSum = Math.log(sum);
		double Z = maxLog + logSum; 
		for(int i = 0; i < data.size(); i++) {
			data.set(i, data.get(i) - Z);
		}
	}
	
	public Factor reduce(ArrayList<Integer> heldVars, ArrayList<Integer> heldValues){
		Factor result = new Factor(this.difference(heldVars));
		
		for(int i = 0; i<result.data.size(); i++){
			try {
				ArrayList<Integer> variablesOfLarger = new ArrayList<Integer>(result._variables);
				variablesOfLarger.addAll(heldVars);
				ArrayList<Integer> varValues = result.valuesFromIndex(i);
				varValues.addAll(heldValues);
				try {
					int indexOfLarger = this.index(variablesOfLarger,varValues);
					result.data.set(i, this.data.get(indexOfLarger));
				}
				catch(FactorIndexException ex) {
					// if we don't find the index of our desired variable
					// (if the original doesn't have it), simply print the original
					result.data.set(i, this.data.get(i));
				}
			} catch (FactorIndexException e) {
				// this is from the 'valuesFromIndex' call,
				// though it will never fail because of the for loop constraint
				e.printStackTrace();
			}
		}
		return result;
	}
	
	/**
	 * Creates and returns a key representation of this factor.
	 * This is a string representation of the variables array list
	 * @return a key representation of this factor
	 */
	protected String makeKey() {
		return _variables.toString();

	}
	
	public static Factor indicatorFunctionForNames(ArrayList<String> varNames, ArrayList<String> valueNames){
		ArrayList<Integer> vars = variableNamesToIndicies(varNames);
		ArrayList<Integer> values = valueNamesToIndicies(varNames,valueNames);
		return indicatorFunction(vars,values);
	}
	
	public static Factor indicatorFunction(int var, int value){
		ArrayList<Integer> vars = new ArrayList<Integer>();
		vars.add(var);
		ArrayList<Integer> values = new ArrayList<Integer>();
		values.add(value);
		return indicatorFunction(vars,values);
	}
	
	public static Factor indicatorFunction(ArrayList<Integer> vars, ArrayList<Integer> values){
		Factor result = new Factor(vars,Math.log(0.0));
		
		for(int i = 0; i<result.data.size(); i++){
			boolean match = true;
			for(int varIdx = 0; varIdx < vars.size(); varIdx++){
				int valueOfVarAtIndex = (i/result._stride.get(varIdx))%_variableCard.get(vars.get(varIdx));
				if(valueOfVarAtIndex != values.get(varIdx)){
					match = false;
					break;
				}
			}
			if(match) result.data.set(i, Math.log(1.0));
			else result.data.set(i, Math.log(0.0));
		}
		
		return result;
	}
	// p555
	public Factor maxMarginalize(ArrayList<Integer> elimVar) throws FactorIndexException {
		ArrayList<Integer> finalVars = difference(elimVar);
//		System.out.println("my vars:"+_variables+" aka "+Factor.variableIndicesToNames(_variables));
//		System.out.println("final vars:"+finalVars+" aka "+Factor.variableIndicesToNames(finalVars));
		Factor result = new Factor(finalVars, 0);
//		System.out.println("\ninital factor of 0's:");
//		System.out.println(result.toString());
//		System.out.println("\nbegin marginalizing");
		
		for(int datum_idx = 0; datum_idx < this.data.size(); datum_idx++ ){
			ArrayList<Integer> values = valuesFromIndex(datum_idx);
			
			int[] f_indicies_of_values = new int[finalVars.size()];
			//ArrayList<Integer> sharedVarValues = new ArrayList<Integer>();
			
			for(int s:finalVars){
				
				int this_ind = this._variables.indexOf(s);//index of variable in sepset in this factor
				//sharedVarValues.add(values[this_ind]);//value of that variable
				int that_ind = result._variables.indexOf(s);//index of variable in sepset in this factor
				f_indicies_of_values[that_ind] = values.get(this_ind);
			}
//			System.out.printf("this:%e result:%f\n", Math.exp(this.data.get(datum_idx)), 
//					Math.exp(result.data.get(result.index(f_indicies_of_values))));
			result.data.set(result.index(f_indicies_of_values), 
					Math.log(
							Math.max(
							Math.exp(result.data.get(result.index(f_indicies_of_values)))
							,Math.exp(this.data.get(datum_idx))
							)));
		}
		return result;
	}	
		
	protected void printData() {
		for(int i = 0; i < data.size(); i++) {
			System.out.print(Math.exp(data.get(i)));
			System.out.print(", ");
		}
		System.out.println();
	}
	
	public static void main(String args[]) throws Exception{
		
//		if(true){
//			System.out.println("\n\nMarginalizes\nexample p297\n");
//		//Margin Test
//		ArrayList<String> A_vals = new ArrayList<String>();
//		A_vals.add("1");
//		A_vals.add("2");
//		A_vals.add("3");
//		
//		ArrayList<String> B_vals = new ArrayList<String>();
//		B_vals.add("1");
//		B_vals.add("2");
//		
//		Factor.addVariable("A", A_vals);
//		Factor.addVariable("B", B_vals);
//		Factor.addVariable("C", B_vals);
//		System.out.println(Factor.variableInfo());
//		
//		String[] fac1_vars = {"A","B","C"}; 
//		Factor fac1 = new Factor(fac1_vars);
//		
//		fac1.putProbByValues(0.25, 0, 0, 0);
//		fac1.putProbByValues(0.35, 0, 0, 1);
//		fac1.putProbByValues(0.08, 0, 1, 0);
//		fac1.putProbByValues(0.16, 0, 1, 1);
//		fac1.putProbByValues(0.05, 1, 0, 0);
//		fac1.putProbByValues(0.07, 1, 0, 1);
//		fac1.putProbByValues(0.00, 1, 1, 0);
//		fac1.putProbByValues(0.00, 1, 1, 1);
//		fac1.putProbByValues(0.15, 2, 0, 0);
//		fac1.putProbByValues(0.21, 2, 0, 1);
//		fac1.putProbByValues(0.09, 2, 1, 0);
//		fac1.putProbByValues(0.18, 2, 1, 1);
//
//		System.out.println(fac1);
//		
//		ArrayList<Integer> elim_vars = new ArrayList<Integer>();
//		elim_vars.add(1);
//		System.out.println("marginalize!");
//		Factor marginialzied = fac1.marginalize(elim_vars);
//		System.out.println(marginialzied);
//
//		}
//		
//		if(true){
//			System.out.println("\n\nDivision\n365\n");
//		//Division Test
//		ArrayList<String> A_vals = new ArrayList<String>();
//		A_vals.add("1");
//		A_vals.add("2");
//		A_vals.add("3");
//		
//		ArrayList<String> B_vals = new ArrayList<String>();
//		B_vals.add("1");
//		B_vals.add("2");
//		
//		ArrayList<String> C_vals = new ArrayList<String>();
//		C_vals.add("1");
//		C_vals.add("2");
//		
//		Factor.addVariable("A", A_vals);
//		Factor.addVariable("B", B_vals);
//		Factor.addVariable("C", C_vals);
//		System.out.println(Factor.variableInfo());
//		
//		String[] fac1_vars = {"A","B"}; 
//		Factor fac1 = new Factor(fac1_vars);
//		fac1.putProbByValues(.5, 0, 0);
//		fac1.putProbByValues(.2, 0, 1);
//		fac1.putProbByValues(0, 1, 0);
//		fac1.putProbByValues(1, 1, 1);
//		fac1.putProbByValues(.3, 2, 0);
//		fac1.putProbByValues(.45, 2, 1);
//		System.out.println(fac1);
//		
//		String[] fac2_vars = {"A"}; 
//		Factor fac2 = new Factor(fac2_vars);
//		fac2.putProbByValues(.8, 0);
//		fac2.putProbByValues(.0, 1);
//		fac2.putProbByValues(.6, 2);
//		System.out.println(fac2);
//		
//		String[] fac3_vars = {"A"}; 
//		Factor fac3 = new Factor(fac3_vars);
//		fac3.putProbByValues(.8, 0);
//		fac3.putProbByValues(.0, 1);
//		fac3.putProbByValues(.6, 2);
//		System.out.println(fac3);
//		
//		System.out.println("f1 / f2 = ");
//		System.out.println(fac1.divide(fac2));
//		}
//		
//		
//		if(true){
//		//Product Test
//			System.out.println("\n\nProduct\np107\n");
//		ArrayList<String> A_vals = new ArrayList<String>();
//		A_vals.add("1");
//		A_vals.add("2");
//		A_vals.add("3");
//		
//
//		ArrayList<String> B_vals = new ArrayList<String>();
//		B_vals.add("1");
//		B_vals.add("2");
//		
//		ArrayList<String> C_vals = new ArrayList<String>();
//		C_vals.add("1");
//		C_vals.add("2");
//		
//		Factor.addVariable("A", A_vals);
//		Factor.addVariable("B", B_vals);
//		Factor.addVariable("C", C_vals);
//		System.out.println(Factor.variableInfo());
//		
//		String[] fac1_vars = {"A","B"};
//		Factor fac1 = new Factor(fac1_vars);
//		fac1.putProbByValues(.5,0,0);
//		fac1.putProbByValues(.8,0,1);
//		fac1.putProbByValues(.1,1,0);
//		fac1.putProbByValues(.0,1,1);
//		fac1.putProbByValues(.3,2,0);
//		fac1.putProbByValues(.9,2,1);
//	
//		System.out.println(fac1);
//		
//		String[] fac2_vars = {"B","C"}; 
//		Factor fac2 = new Factor(fac2_vars);
//		fac2.putProbByValues(.5, 0,0);
//		fac2.putProbByValues(.7, 0,1);
//		fac2.putProbByValues(.1, 1,0);
//		fac2.putProbByValues(.2, 1,1);
//
//		System.out.println(fac2);
//		System.out.println(fac2.data.toString());
//		System.out.println("A x B = ");
//		System.out.println(fac1.product(fac2));
//		}
//		
//		if(true){
//		//Reduction test p 107 and 111
//			System.out.println("\n\nreduction\np 107 and 111\n");
//		ArrayList<String> A_vals = new ArrayList<String>();
//		A_vals.add("1");
//		A_vals.add("2");
//		A_vals.add("3");
//		
//		ArrayList<String> B_vals = new ArrayList<String>();
//		B_vals.add("1");
//		B_vals.add("2");
//		
//		ArrayList<String> C_vals = new ArrayList<String>();
//		C_vals.add("1");
//		C_vals.add("2");
//		
//		ArrayList<String> D_vals = new ArrayList<String>();
//		D_vals.add("1");
//		D_vals.add("2");
//		
//		Factor.addVariable("A", A_vals);
//		Factor.addVariable("B", B_vals);
//		Factor.addVariable("C", C_vals);
//		Factor.addVariable("D", D_vals);
//		System.out.println(Factor.variableInfo());
//		
//		String[] fac1_vars = {"A","B","C"}; 
//		Factor fac1 = new Factor(fac1_vars);
//		fac1.putProbByValues(.25, 0,0,0);// 1 1 1
//		fac1.putProbByValues(.05, 1,0,0);// 2 1 1
//		fac1.putProbByValues(.15, 2,0,0);// 3 1 1
//		
//		fac1.putProbByValues(.08, 0,1,0);// 1 2 1 
//		fac1.putProbByValues(0, 1,1,0);  // 2 2 1
//		fac1.putProbByValues(.09, 2,1,0);// 3 2 1
//		
//		fac1.putProbByValues(.35, 0,0,1);// 1 1 2
//		fac1.putProbByValues(.07, 1,0,1);// 2 1 2
//		fac1.putProbByValues(.21, 2,0,1);// 3 1 2
//		
//		fac1.putProbByValues(.16, 0,1,1); //1 2 2
//		fac1.putProbByValues(0, 1,1,1);  //2 2 2
//		fac1.putProbByValues(.18, 2,1,1);//3 2 2
//		System.out.println(fac1);
//		{
//		System.out.println("reduce f1 by C=1");
//		ArrayList<String> heldVarStrs = new ArrayList<String>();
//		heldVarStrs.add("C");
//		ArrayList<String> heldVarValStrs = new ArrayList<String>();
//		heldVarValStrs.add("1");
//		ArrayList<Integer> heldVars = Factor.variableNamesToIndicies(heldVarStrs);
//		ArrayList<Integer> heldValues = Factor.valueNamesToIndicies(heldVarStrs, heldVarValStrs);
//		System.out.println(fac1.reduce(heldVars, heldValues));
//		}
//		
//			if(true){
//				System.out.println("reduce f1 by D=1");
//				ArrayList<String> heldVarStrs = new ArrayList<String>();
//				heldVarStrs.add("D");
//				ArrayList<String> heldVarValStrs = new ArrayList<String>();
//				heldVarValStrs.add("1");
//				ArrayList<Integer> heldVars = Factor.variableNamesToIndicies(heldVarStrs);
//				ArrayList<Integer> heldValues = Factor.valueNamesToIndicies(heldVarStrs, heldVarValStrs);
//				System.out.println(fac1.reduce(heldVars, heldValues));
//			}
//		}
//		if(true) {
//			System.out.println("\n\nnormalize test\n");
//			ArrayList<String> X_vals = new ArrayList<String>();
//			X_vals.add("1");
//			X_vals.add("2");
////			X_vals.add("3");
//			
//			ArrayList<String> Y_vals = new ArrayList<String>();
//			Y_vals.add("1");
//			Y_vals.add("2");
//			
//			ArrayList<String> Z_vals = new ArrayList<String>();
//			Z_vals.add("1");
//			Z_vals.add("2");
//			
//			Factor.addVariable("X", X_vals);
//			Factor.addVariable("Y", Y_vals);
//			Factor.addVariable("Z", Z_vals);
//			System.out.println(Factor.variableInfo());
//			
//			String[] fac1_vars = {"X","Y","Z"}; 
//			Factor fac1 = new Factor(fac1_vars);
//			fac1.putProbByValues(65, 0,0,0);
//			fac1.putProbByValues(2, 0,0,1);
//			fac1.putProbByValues(3, 0,1,0);
//			fac1.putProbByValues(4, 0,1,1);
//			fac1.putProbByValues(5, 1,0,0);
//			fac1.putProbByValues(6, 1,0,1);
//			fac1.putProbByValues(7, 1,1,0);
//			fac1.putProbByValues(8, 1,1,1);
////			fac1.putProbByValues(9, 2,0,0);
////			fac1.putProbByValues(10, 2,0,1);
////			fac1.putProbByValues(11, 2,1,0);
////			fac1.putProbByValues(12, 2,1,1);
//			System.out.println("before normalize:");
//			System.out.println(fac1);
//			System.out.println("normalize:");
//			fac1.normalize();
//			System.out.println(fac1);
//		}
		if(true) {
			System.out.println("maximization test p555");
			ArrayList<String> A_vals = new ArrayList<String>();
			A_vals.add("1");
			A_vals.add("2");
			A_vals.add("3");
			
			ArrayList<String> B_vals = new ArrayList<String>();
			B_vals.add("1");
			B_vals.add("2");
			
			ArrayList<String> C_vals = new ArrayList<String>();
			C_vals.add("1");
			C_vals.add("2");
			
			Factor.addVariable("A", A_vals);
			Factor.addVariable("B", B_vals);
			Factor.addVariable("C", C_vals);
			System.out.println(Factor.variableInfo());
			
			String[] fac1_vars = {"A","B","C"}; 
			Factor fac1 = new Factor(fac1_vars);
			fac1.putProbByValues(.25, 0,0,0);// 1 1 1
			fac1.putProbByValues(.05, 1,0,0);// 2 1 1
			fac1.putProbByValues(.15, 2,0,0);// 3 1 1
			
			fac1.putProbByValues(.08, 0,1,0);// 1 2 1 
			fac1.putProbByValues(0, 1,1,0);  // 2 2 1
			fac1.putProbByValues(.09, 2,1,0);// 3 2 1
			
			fac1.putProbByValues(.35, 0,0,1);// 1 1 2
			fac1.putProbByValues(.07, 1,0,1);// 2 1 2
			fac1.putProbByValues(.21, 2,0,1);// 3 1 2
			
			fac1.putProbByValues(.16, 0,1,1); //1 2 2
			fac1.putProbByValues(0, 1,1,1);  //2 2 2
			fac1.putProbByValues(.18, 2,1,1);//3 2 2
			System.out.println(fac1);
			
			ArrayList<Integer> elimVar = new ArrayList<Integer>();
			elimVar.add(1);
			Factor result = fac1.maxMarginalize(elimVar);
			System.out.println("f1 maximized by B:");
			System.out.println(result);
			elimVar.add(2);
			result = fac1.maxMarginalize(elimVar);
			System.out.println("f1 maximized by B,C:");
			System.out.println(result);
			
			String[] fac2_vars = {"C"}; 
			Factor fac2 = new Factor(fac2_vars);
			fac2.putProbByValues(.5, 0); //1 2 2
			fac2.putProbByValues(.06, 1);  //2 2 2
			System.out.println("fac2:");
			System.out.println(fac2);
			elimVar.clear();
			elimVar.add(1);
			System.out.println("f2 maximized by B:");
			System.out.println(fac2.maxMarginalize(elimVar));
			elimVar.clear();
			elimVar.add(2);
			System.out.println("f2 maximized by C:");
			System.out.println(fac2.maxMarginalize(elimVar));
			elimVar.clear();
			System.out.println("f2 maximized by \\empty:");
			System.out.println(fac2.maxMarginalize(elimVar));
		}
	}
	
}
