package elanmike.mlcd.hw2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import elanmike.mlcd.hw2.Factor.FactorException;
import elanmike.mlcd.hw2.Factor.FactorIndexException;

/**
 * Belief Update Message Passing class
 * 
 * @author mcs
 *
 */
public class Bump {
	public static final int UNMARKED = 0;
	public static int nextOrderID = UNMARKED;
	/**
	 * Small clique class that holds a list of variables
	 * We compare cliques by their number of shared variables
	 * @author mcs
	 *
	 */
	protected class Clique extends Factor {
		
		Clique(String[] varNames) {
			super(varNames);
		}
		
		protected Clique(Clique cliqueToCopy) {
			super(cliqueToCopy);
		}
		
		/**
		 * A clique is equal to another clique if their sets of variables are equal
		 */
		@Override
		public boolean equals(Object other) {
			if(other instanceof Clique) {
				Clique c = (Clique) other;
				return c._variables.equals(this._variables);
			}
			return false;
		}
		public String toString() {
			return Factor.variableIndicesToNames(_variables).toString();
		}
		public String getLongInfo() {
			StringBuilder sb = new StringBuilder(toString());
			// add factor result
			sb.append("\n").append(super.toString());
			return sb.toString();
		}
	}
	protected class Vertex extends Clique {
		private int _orderID;
		private Set<Edge> _outgoingEdges;
		/**
		 * Stores each neighboring edge, initializes with the fact that we
		 * have not yet received an informed message via that edge.
		 * Tracks our 'informed' status - whether or not we've received
		 * informed messages on all edges
		 */
		private Map<Edge, Boolean> _recvdMsgStatus;
		private boolean _isInformed;
		Vertex(String[] varsContained){
			super(varsContained);
			_outgoingEdges = new HashSet<Edge>();
			_recvdMsgStatus = new HashMap<Edge, Boolean>();
			reset();
		}
		
		protected Vertex(Vertex vertexToCopy) {
			super(vertexToCopy);
			_outgoingEdges = new HashSet<Edge>();
			_recvdMsgStatus = new HashMap<Edge, Boolean>();
			reset();
		}
		
		/**
		 * Resets this vertex to prepare for the running of the algorithm
		 */
		void reset() {
			_orderID = UNMARKED;
			_isInformed = false;
			for(Edge e : _recvdMsgStatus.keySet()) {
				_recvdMsgStatus.put(e, false);
			}
			_outgoingEdges.clear();
		}
		void setOrderID() {
			this._orderID = ++nextOrderID;
			if(DEBUG) System.out.println("marked "+this.toString() + " with id:"+_orderID);
		}
		void setUnmarked() {this._orderID = UNMARKED;}
		/**
		 * Adds a neighbor edge
		 * checks for duplicates.
		 * initializes our received message status for that edge.
		 * @param e
		 */
		void addNeighborEdge(Edge e) {
			if(!_recvdMsgStatus.containsKey(e)) {
				_recvdMsgStatus.put(e, false);
			}
		}
		/**
		 * Sends a message to its neighbor.
		 * Calls the neighbor's onReceiveMessage method
		 * @param edgeToJ
		 * @throws FactorIndexException 
		 */
		void sendMessage(Edge edgeToJ) throws FactorException {
			if(DEBUG) System.out.println(this.toString()+" sending message to:"+edgeToJ.getOtherVertex(this)+"\n via edge:"+edgeToJ);
			// calculate message - marginalize out all variables not in sepset ij
			if(DEBUG) System.out.println("elim:"+this.difference(edgeToJ._variables)+" aka "+Factor.variableIndicesToNames(this.difference(edgeToJ._variables)));
			if(DEBUG) System.out.println("belief before marginalize:");
			if(DEBUG) System.out.println(super.getLongInfo());
			Factor sigmaItoJ;
			if(_useSumProduct) {
				if(DEBUG) System.out.println("using sum product");
				sigmaItoJ = this.marginalize(this.difference(edgeToJ._variables));
			}
			else {
				if(DEBUG) System.out.println("using max product");
				sigmaItoJ = this.maxMarginalize(this.difference(edgeToJ._variables));
			}
			if(DEBUG) System.out.println("sigma I,J:\n"+sigmaItoJ);
			// send: make J receive
			edgeToJ.getOtherVertex(this).onReceiveMessage(edgeToJ, sigmaItoJ);
			// update edge potential
			edgeToJ.setFactorData(sigmaItoJ);
			// update edge message count
			edgeToJ._timesMessagesSentAcrossMe++;
			if(DEBUG) System.out.println("mu I,J:\n"+edgeToJ.getLongInfo());
		}
		/**
		 * When we receive a message...
		 * (called by our 'send message' function when our neighbor sends
		 * @param edgeItoJ
		 * @param sigmaItoJ
		 */
		private void onReceiveMessage(Edge edgeItoJ, Factor sigmaItoJ) throws FactorException {
			if(DEBUG) System.out.println(this.toString()+" receiving msg from:"+edgeItoJ.getOtherVertex(this));
			if(DEBUG) System.out.println("old belief:");
			if(DEBUG) System.out.println(super.getLongInfo());
			// belief j = belief j * (sigma ij / mu ij)
			if(DEBUG) System.out.println("mu ItoJ:"+edgeItoJ.getLongInfo());
			this.setFactorData(this.product(sigmaItoJ.divide(edgeItoJ)));
			if(DEBUG) System.out.println("belief J:\n"+super.getLongInfo());
			// check if I was informed when sending
			Vertex i = edgeItoJ.getOtherVertex(this);
//			_recvdMsgStatus.put(edgeItoJ, i._isInformed);
			_recvdMsgStatus.put(edgeItoJ, true);
			if(i._isInformed) { // if vertex I was informed....
				_isInformed = isInformed(); // recheck if we are informed
			}
//			if(DEBUG) System.out.println("informed status:\n"+_recvdMsgStatus);
		}
		/**
		 * Return a list of all outgoing neighbors for the upward pass,
		 * as defined by our vertex ordering.
		 * @return the list of all outgoing neighbors for the upward pass
		 */
		Set<Edge> getDownwardOutgoingNeighborEdges() {
			if(!_bumpOnUpwardPass) {
				System.err.println("calling upward pass neighbor method on downward pass!");
			}
//			if(_outgoingEdges.size() == 0) { // only compute if we have to
				Set<Edge> outgoingEdges = new HashSet<Edge>();
				Iterator<Edge> it = _recvdMsgStatus.keySet().iterator();
				while(it.hasNext()) {
					Edge e = it.next();
					Vertex v = e.getOtherVertex(this);
					if(this._orderID == UNMARKED) {
						System.err.println("whoops i am unmarked");
					}
					if(v._orderID == UNMARKED) {
						System.err.println("whoops neighbor unmarked");
					}
					if(this._orderID < v._orderID) {
						outgoingEdges.add(e);
					}
				}
				_outgoingEdges = outgoingEdges;
//			}
//			return _outgoingEdges;
				return outgoingEdges;
		}
		/**
		 * Return a list of all outgoing neighbors for the upward pass,
		 * as defined by our vertex ordering.
		 * @return the list of all outgoing neighbors for the upward pass
		 */
		Set<Edge> getUpwardOutgoingNeighborEdges() {
			if(!_bumpOnUpwardPass) {
				System.err.println("calling upward pass neighbor method on downward pass!");
			}
//			if(_outgoingEdges.size() == 0) { // only compute if we have to
				Set<Edge> outgoingEdges = new HashSet<Edge>();
				Iterator<Edge> it = _recvdMsgStatus.keySet().iterator();
				while(it.hasNext()) {
					Edge e = it.next();
					Vertex v = e.getOtherVertex(this);
					if(this._orderID == UNMARKED) {
						System.err.println("whoops i am unmarked");
					}
					if(v._orderID == UNMARKED) {
						System.err.println("whoops neighbor unmarked");
					}
					if(this._orderID > v._orderID) {
						outgoingEdges.add(e);
					}
				}
				_outgoingEdges = outgoingEdges;
//			}
//			return outgoingEdges;
				return outgoingEdges;
		}
		/**
		 * Checks if this vertex is informed.
		 * @return true if has received informed messages from all neighbors
		 */
		boolean isInformed() {
			// default to true, to account for the case where we have no neighbors
			for(boolean b : _recvdMsgStatus.values()) {
				// if any value is false, we return false
				if(!b) return false; 
			}
			return true;
		}
		@Override
		public boolean equals(Object o) {
			if(o instanceof Vertex) {
				Vertex v = (Vertex) o;
				return v._variables == this._variables;
			}
			return false;
		}
		public String getLongInfo() {
			StringBuilder sb = new StringBuilder("Vertex:\n");
			// add clique string
			sb.append(super.getLongInfo());
			// add edge info
			sb.append("\nrecvdMsgStatus:").append(_recvdMsgStatus);
			return sb.toString();
		}
	}
	private class Edge extends Factor{
		public static final String EDGE = " -- ";
		Vertex _one, _two;
		private int _timesMessagesSentAcrossMe;
		Edge(Vertex one, Vertex two) {
			super(one.intersection(two._variables));
			this._one = one;
			this._two = two;
			_timesMessagesSentAcrossMe = 0;
		}
		
		protected Edge(Edge edgeToCopy, Vertex newOne, Vertex newTwo) {
			super(edgeToCopy);
			this._one = newOne;
			this._two = newTwo;
			_timesMessagesSentAcrossMe = 0;
		}

		/**
		 * An edge is equal to another edge if its two vertices match the other's two
		 */
		@Override
		public boolean equals(Object other) {
			if(other instanceof Edge) {
				Edge e = (Edge) other;
				return e._one.equals(this._one) && e._two.equals(this._two)
					|| e._one.equals(this._two) && e._two.equals(this._one);
			}
			return false;
		}
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(Factor.variableIndicesToNames(_one._variables));
			sb.append(EDGE);
			sb.append(Factor.variableIndicesToNames(_two._variables));
			return sb.toString();
		}
		public String getLongInfo() {
			StringBuilder sb = new StringBuilder("edge:\n");
			sb.append("used:").append(_timesMessagesSentAcrossMe).append(" times for messages.\n");
			sb.append(Factor.variableIndicesToNames(_one._variables));
			sb.append(" --").append(Factor.variableIndicesToNames(_variables)).append("-- ");
			sb.append(Factor.variableIndicesToNames(_two._variables));
			sb.append("\nmu:\n").append(super.toString());
			return sb.toString();
		}
		/**
		 * Given a vertex v, if this edge contains v, get the other vertex in the edge.
		 * else return null, if this edge doesn't contain v
		 * @param v
		 * @return
		 */
		Vertex getOtherVertex(Vertex v) {
			if(v.equals(_one)) return _two;
			else if(v.equals(_two)) return _one;
			else return null;
		}
	}
	private class Tree {
		private HashMap<String,Vertex> _vertices;
		private HashMap<String,Edge> _edges;
		Tree() {
			_vertices = new HashMap<String,Vertex>();
			_edges = new HashMap<String,Edge>();
		}
		protected Tree(Tree other) {
			_vertices = new HashMap<String,Vertex>();
			Iterator<Entry<String, Vertex>> itv = other._vertices.entrySet().iterator();
			while(itv.hasNext()) {
				Entry<String,Vertex> entry = itv.next();
				_vertices.put(entry.getKey(), new Vertex(entry.getValue()));
			}
			_edges = new HashMap<String,Edge>();
			Iterator<Entry<String, Edge>> ite = other._edges.entrySet().iterator();
			while(ite.hasNext()) {
				Entry<String, Edge> entry = ite.next();
				// pass old edge, and new versions of old vertices
				// get keys of old vertices
				String keyOne = entry.getValue()._one.makeKey(), 
						keyTwo = entry.getValue()._two.makeKey();
				this.addEdge(new Edge(entry.getValue(), 
					_vertices.get(keyOne), _vertices.get(keyTwo)));
			}
		}
		void addVertex(Vertex v) {
			_vertices.put(v.makeKey(),v);
			//System.out.println("adding vertice with key: "+ v._variables.toString());
		}
		/**
		 * Adds the edge to the tree.
		 * Checks for duplicates edges
		 * @param e
		 */
		void addEdge(Edge e) {
			if(!_edges.containsKey(e.makeKey())) {
				//_vertices.put(e._one._variables.toString(),e._one);
				//_vertices.put(e._two._variables.toString(),e._two);
				_edges.put(e.makeKey(),e);
				// for each involved vertex V, add the edge to its 'informed' check
				e._one.addNeighborEdge(e);
				e._two.addNeighborEdge(e);
			}
		}
		public String toString(){
			StringBuilder output = new StringBuilder("=== Vertices ===\n");
			Set<String> keys = _vertices.keySet();
			for(String k:keys){
				output.append(_vertices.get(k).toString()).append("\n\n");
			}
			output.append("=== Edges ===\n");
			keys = _edges.keySet();
			for(String k:keys){
				output.append(_edges.get(k).toString()).append("\n");
			}
			return output.toString();
		}
		public String getLongInfo() {
			StringBuilder output = new StringBuilder("=== Vertices ===\n");
			Set<String> keys = _vertices.keySet();
			for(String k:keys){
				output.append(_vertices.get(k).getLongInfo()).append("\n\n");
			}
			output.append("=== Edges ===\n");
			keys = _edges.keySet();
			for(String k:keys){
				output.append(_edges.get(k).getLongInfo()).append("\n");
			}
			return output.toString();
		}
	}
	public static final boolean DEBUG = false;
	/**
	 * true if we're on the upward pass, 
	 * if we're going in increasing order id. 
	 * false otherwise
	 */
	protected boolean _bumpOnUpwardPass;
	private Tree _tree, _queryTree;
	private boolean _useSumProduct;
	public Bump() {
		_tree = new Tree();
		_bumpOnUpwardPass = false;
		_useSumProduct = false;
	}
	
	public void init(String networkFilename,String cliqueTreeFilename, String cpdFilename) throws IOException, ArrayIndexOutOfBoundsException, IllegalArgumentException, FactorException{
		readNetworkFile(networkFilename);
		readCliqueTreeFile(cliqueTreeFilename);
		readCPDFile(cpdFilename);
	}
	
	/**
	 * @return use sum product
	 */
	public boolean useSumProduct() {return _useSumProduct;}
	/**
	 * Sets the value of use sum product - this determines our semiring.
	 * Note that after changing this, one needs to call runBump again
	 * to recalibrate the tree.
	 * @param useSumProduct true if use sum product, false if use max product
	 */
	void setUseSumProduct(boolean useSumProduct) {
		_useSumProduct = useSumProduct;
	}
	/**
	 * Runs belief update message passing to calibrate the tree.
	 * Resets, inits beliefs, calibrates the tree.
	 * copies this calibrated tree such that we might alter it with query evidence
	 * @return true if successful, false otherwise
	 */
	public boolean runBump() {
		if(DEBUG) System.out.println("running bump!");
		// note we initialize the clique tree by construction!
		try {
			List<Vertex> ordering = assignBumpOrdering(_tree);
			if(DEBUG) System.out.printf("ordering:\n%s\n", ordering);
			upwardPassBeliefUpdate(ordering);
			downwardPassBeliefUpdate(ordering);
//			upwardPassBeliefUpdate(downwardPassBeliefUpdate(_tree));
//			upwardPassBeliefUpdate(ordering);
//			downwardPassBeliefUpdate(ordering);
//			ordering = assignBumpOrdering(_tree);
//			upwardPassBeliefUpdate(ordering);
//			downwardPassBeliefUpdate(ordering);
		} catch (FactorException e) {
			e.printStackTrace();
			return false;
		}
		if(DEBUG) {
			if(DEBUG) System.out.println("\n\n******\nis tree calibrated?\n\n");
			if(DEBUG) System.out.println(isCalibrated());
			if(DEBUG) System.out.println(_tree.getLongInfo());
		}
		else {
			isCalibrated();
		}
		return true;
	}
	
	public boolean isCalibrated() {
		boolean passed = true;
		if(DEBUG) System.out.println("checking edges:");
		for(Edge curEdge : _tree._edges.values()) {
			try {
				Factor one,two;
				if(_useSumProduct) {
					one = curEdge._one.marginalize(curEdge._one.difference(curEdge._variables));
					two = curEdge._two.marginalize(curEdge._two.difference(curEdge._variables));
				}
				else {
					one = curEdge._one.maxMarginalize(curEdge._one.difference(curEdge._variables));
					two = curEdge._two.maxMarginalize(curEdge._two.difference(curEdge._variables));	
				}
				
				// check sets of variables
				Set<Integer> sone = new TreeSet<Integer>(one._variables);
				Set<Integer> stwo = new TreeSet<Integer>(two._variables);
				Set<Integer> sedge = new TreeSet<Integer>(curEdge._variables);
				if(!sone.equals(stwo) || !stwo.equals(sedge) || !sone.equals(sedge)) {
					System.err.println("'calibrated' clique sepsets / edge sets not equal");
					passed = false;
					break;
				}
				// check number of times edge was used:
				if(curEdge._timesMessagesSentAcrossMe != 2) {
					System.err.printf("'calibrated' edge used for messages:%d times\n", curEdge._timesMessagesSentAcrossMe);
					passed = false; 
					break;
				}
				if(one.data.size() != two.data.size()){
					System.err.println("'calibrated' clique marginal factors not equal size");
					passed =  false;
					break;
				}
				if(one.data.size() != curEdge.data.size()){
					System.err.println("'calibrated' edge / clique factors not equal size");
					passed =  false;
					break;
				}
			}
			catch(FactorException ex) {
				ex.printStackTrace();
			}
		}
		if(DEBUG) System.out.println("checking edge values:");
		for(String e:_tree._edges.keySet()){
			Edge curEdge = _tree._edges.get(e);
			
			try {
				Factor one,two;
				if(_useSumProduct) {
					one = curEdge._one.marginalize(curEdge._one.difference(curEdge._variables));
					two = curEdge._two.marginalize(curEdge._two.difference(curEdge._variables));
				}
				else {
					one = curEdge._one.maxMarginalize(curEdge._one.difference(curEdge._variables));
					two = curEdge._two.maxMarginalize(curEdge._two.difference(curEdge._variables));	
				}
				// check sets of variables
				Set<Integer> sone = new TreeSet<Integer>(one._variables);
				Set<Integer> stwo = new TreeSet<Integer>(two._variables);
				Set<Integer> sedge = new TreeSet<Integer>(curEdge._variables);
				if(!sone.equals(stwo) || !stwo.equals(sedge) || !sone.equals(sedge)) {
					System.err.println("'calibrated' clique sepsets / edge sets not equal");
					passed = false;
//					break;
				}
				// check number of times edge was used:
				if(curEdge._timesMessagesSentAcrossMe != 2) {
					System.err.printf("'calibrated' edge used for messages:%d times\n", curEdge._timesMessagesSentAcrossMe);
					passed = false; 
//					break;
				}
				if(one.data.size() != two.data.size()){
					System.err.println("'calibrated' clique marginal factors not equal size");
					passed =  false;
//					break;
				}
				if(one.data.size() != curEdge.data.size()){
					System.err.println("'calibrated' edge / clique factors not equal size");
					passed =  false;
//					break;
				}
				one.normalize();
				two.normalize();
				for(int i = 0; i<one.data.size(); i++){
					//if(!one.data.get(i).equals(two.data.get(i))){
					float a = ((Double)Math.exp(one.data.get(i))).floatValue(),
						b = ((Double)Math.exp(two.data.get(i))).floatValue();
					if(a != b) {
						System.err.printf("\ndata at index %d is not equal (%f = %f)\n",i,Math.exp(one.data.get(i)),Math.exp(two.data.get(i)));
						System.err.println("Factor from clique:"+curEdge._one);
						System.err.println("one:");
						System.err.println(one.toString());
						System.err.println("Factor from clique:"+curEdge._two);
						System.err.println("two:");
						System.err.println(two.toString());
						System.err.println("edge:");
						System.err.println(curEdge.getLongInfo());
						passed =  false;
//						break;
					}
				}
			} catch (FactorIndexException e1) {
				e1.printStackTrace();
				return false;
			}
		}
		
		return passed;
	}
	
	/**
	 * Conducts one downward pass of belief update message passing
	 * with a designated root.
	 * Conducts breadth-first search of the query tree, 
	 * and sends messages in that order.
	 * 
	 * Chooses an arbitrary root.
	 * 
	 * @param t the tree in question
	 * @return a list of the order of this pass, so we can reverse it in the upward pass
	 * @throws FactorException 
	 */
	List<Vertex> assignBumpOrdering(Tree t) throws FactorException {
		return assignBumpOrdering(t, t._vertices.values().iterator().next());
	}
	/**
	 * Conducts one downward pass of belief update message passing
	 * with a designated root.
	 * Conducts depth-first or breadth-first search of the query tree, 
	 * sends messages in that order.
	 * 
	 * @param t the tree in question.
	 * @param root
	 * @return a list of the order of this pass, so the upward pass can reverse it
	 * @throws FactorException 
	 */
	List<Vertex> assignBumpOrdering(Tree t, Vertex root) throws FactorException {
		_bumpOnUpwardPass = false;
		if(DEBUG) System.out.println("\n\nassigning ordering!\n\n");
		nextOrderID = UNMARKED; // begin again at 0
		List<Vertex> ordering = new ArrayList<Vertex>();
		if(t._vertices.size() == 0) return ordering;
		// mark all vertices unmarked
		for(Vertex v : t._vertices.values()) {
			v.setUnmarked();
		}
		Queue<Vertex> toProcess = new LinkedList<Vertex>();
		toProcess.add(root);
		if(DEBUG) System.out.println("root is "+root.getVariableNames());
		
		// giving a number is equivalent to adding to ordering, giving index
		// while all vertices don't have a number
		while(ordering.size() != t._vertices.size()) {
			if(toProcess.size() == 0) {
				// find an unmarked node... expensive
				for(Vertex v : t._vertices.values()) {
					if(v._orderID == UNMARKED) {
						toProcess.add(v); // add to queue when find unmarked
					}
				}
			}
			Vertex curr = toProcess.remove();
			if(DEBUG) System.out.printf("curr vertex:%s\n",curr);
			// mark
			curr.setOrderID();
			ordering.add(curr); // and add to our ordered list
			if(curr._orderID != ordering.size()) {// verify
				System.err.println("oops! order id incorrect");
			}
			// and then for each downstream child...
			for(Edge e : curr._recvdMsgStatus.keySet()) {
				Vertex k = e.getOtherVertex(curr);
				if(DEBUG) System.out.println("check neighbor:"+k);
				if(k._orderID == UNMARKED) {
					if(DEBUG) System.out.println(k+" is unmarked - it's downstream");
					// downstream if we haven't marked it yet
//					// send belief update message to the child
//					curr.sendMessage(e);
					// and add the child to our list to process
					if(DEBUG) System.out.printf("adding %s to be processed\n",root.getVariableNames());
					toProcess.add(k);
				}
			}
		}
//		Vertex v = findVertexInTree(t, 3,2);
//		v.setOrderID();
//		ordering.clear();
//		ordering.add(v);
//		v = findVertexInTree(t, 2,0);
//		v.setOrderID();
//		ordering.add(v);
//		v = findVertexInTree(t, 1,0);
//		v.setOrderID();
//		ordering.add(v);
		return ordering;
	}
	
	/**
	 * Conducts one downward pass of belief update message passing
	 * with a designated root.
	 * Conducts breadth-first search of the query tree, 
	 * and sends messages in that order.
	 * 
	 * Chooses an arbitrary root.
	 * 
	 * @param t the tree in question
	 * @return a list of the order of this pass, so we can reverse it in the upward pass
	 * @throws FactorException 
	 */
	List<Vertex> downwardPassBeliefUpdate(Tree t) throws FactorException {
		return downwardPassBeliefUpdate(t, t._vertices.values().iterator().next());
	}
	/**
	 * Conducts one downward pass of belief update message passing
	 * with a designated root.
	 * Conducts depth-first or breadth-first search of the query tree, 
	 * sends messages in that order.
	 * 
	 * @param t the tree in question.
	 * @param root
	 * @return a list of the order of this pass, so the upward pass can reverse it
	 * @throws FactorException 
	 */
	List<Vertex> downwardPassBeliefUpdate(Tree t, Vertex root) throws FactorException {
		_bumpOnUpwardPass = false;
		if(DEBUG) System.out.println("\n\ndownward pass!\n\n");
		nextOrderID = UNMARKED; // begin again at 0
		List<Vertex> ordering = new ArrayList<Vertex>();
		if(t._vertices.size() == 0) return ordering;
		// mark all vertices unmarked
		for(Vertex v : t._vertices.values()) {
			v.setUnmarked();
		}
		Queue<Vertex> toProcess = new LinkedList<Vertex>();
		toProcess.add(root);
		// giving a number is equivalent to adding to ordering, giving index
		// while all vertices don't have a number
		while(ordering.size() != t._vertices.size()) {
			if(toProcess.size() == 0) {
				// find an unmarked node... expensive
				for(Vertex v : t._vertices.values()) {
					if(v._orderID == UNMARKED) {
						toProcess.add(v); // add to queue when find unmarked
					}
				}
			}
			Vertex curr = toProcess.remove();
			if(DEBUG) System.out.printf("curr vertex:%s\n",curr);
			// mark
			curr.setOrderID();
			ordering.add(curr); // and add to our ordered list
			if(curr._orderID != ordering.size()) {// verify
				System.err.println("oops! order id incorrect");
			}
			// and then for each downstream child...
			for(Edge e : curr._recvdMsgStatus.keySet()) {
				Vertex k = e.getOtherVertex(curr);
				if(DEBUG) System.out.println("check neighbor:"+k);
				if(k._orderID == UNMARKED) {
					if(DEBUG) System.out.println("k is unmarked - it's downstream");
					// downstream if we haven't marked it yet
					// send belief update message to the child
					curr.sendMessage(e);
					// and add the child to our list to process
					toProcess.add(k);
				}
			}
		}
		return ordering;
	}
	/**
	 * Calibrate the tree with the upward pass of belief-update message passing.
	 * Use the reverse of our ordering that we created in the downward pass.
	 * @throws FactorException 
	 */
	void downwardPassBeliefUpdate(List<Vertex> orderedVertices) throws FactorException {
		_bumpOnUpwardPass = true;
		if(DEBUG) System.out.println("\n\ndownward pass!\n\n");
		for(int i = 0; i < orderedVertices.size(); i++) {
			Vertex v = orderedVertices.get(i);
			if(DEBUG) System.out.printf("i:%d vertex:%s\n",i,v);
			// for each edge that is outgoing given our ordering
			for(Edge e : v.getDownwardOutgoingNeighborEdges()) {
				// send our message along that edge
				v.sendMessage(e);
			}
		}
	}
	/**
	 * Calibrate the tree with the upward pass of belief-update message passing.
	 * Use the reverse of our ordering that we created in the downward pass.
	 * @throws FactorException 
	 */
	void upwardPassBeliefUpdate(List<Vertex> orderedVertices) throws FactorException {
		_bumpOnUpwardPass = true;
		if(DEBUG) System.out.println("\n\nupward pass!\n\n");
		for(int i = orderedVertices.size() - 1; i >= 0; i--) {
			Vertex v = orderedVertices.get(i);
			if(DEBUG) System.out.printf("i:%d vertex:%s\n",i,v);
			// for each edge that is outgoing given our ordering
			for(Edge e : v.getUpwardOutgoingNeighborEdges()) {
				// send our message along that edge
				v.sendMessage(e);
			}
		}
	}
	/**
	 * Resets the query tree in preparation for queries.
	 * makes a fresh copy of the tree in our query tree variable,
	 * and clears our stored contexts.
	 */
	void resetTreeForQueries() {
		_queryTree = new Tree(_tree);
		if(DEBUG) {
			System.out.printf("\nquery tree structure:\n%s\n",_queryTree.toString());
		}
	}
	/**
	 * find a vertex with a clique containing the given set of variables
	 * in the given tree
	 * @param t the tree in question
	 * @param vars the variable integers
	 * @return a suitable vertex, or null otherwise.  should always return a vertex.
	 */
	Vertex findVertexInTree(Tree t, int... vars) {
		for(Vertex v : t._vertices.values()) {
			boolean containsAll = true;
			for(int i : vars) {
				if(!v._variables.contains(i)) {
					containsAll = false;
					//continue; // skip.
				}
			}
			if(containsAll) return v;
		}
		return null;
	}
	/**
	 * find a vertex with a clique containing the given set of variables
	 * in the given tree
	 * @param t the tree in question
	 * @param vars the variable integers
	 * @return a suitable vertex, or null otherwise.  should always return a vertex.
	 */
	Vertex findVertexInTree(Tree t, ArrayList<Integer> vars) {
		for(Vertex v : t._vertices.values()) {
			boolean containsAll = true;
			for(int i : vars) {
				if(!v._variables.contains(i)) {
					containsAll = false;
					//continue; // skip.
				}
			}
			if(containsAll) return v;
		}
		return null;
	}
	/**
	 * 
	/**
	 * Given that our tree is calibrated, incorporate the evidence
	 * into our QUERY COPY.
	 * If the evidence is retractive, we will need to calibrate.
	 * Else we need only one pass, downward from our target vertex.
	 * 
	 * @param vars
	 * @param values
	 * @param treeNeedsToBeCalibrated
	 * @throws FactorException
	 */
	public void incorporateQueryEvidence(ArrayList<Integer> vars, 
			ArrayList<Integer> values, int numNewEvidence) 
			throws FactorException {
		if(DEBUG) System.out.println("incorporate query evidence...");
		if(vars.size() != values.size()) {
			System.err.println("uh oh! vars size must equal values size");
		}
		for(int i = 0; i < vars.size(); i++) {
			int var = vars.get(i), value = values.get(i);
			// Find a clique with the variable, C, in the query tree
			Vertex v = findVertexInTree(_queryTree, var);
			if(v == null) {
				System.err.println("can't find vertex - whoops!");
			}
			// multiply in a new indicator factor
			Factor indicator = Factor.indicatorFunction(var, value);
			v.setFactorData(v.product(indicator));
			if(numNewEvidence == 1) {
				// run only one pass of bump
				if(DEBUG) System.out.println(+numNewEvidence+" new var-> run one pass");
				downwardPassBeliefUpdate(_queryTree,v);
				return;
			} // else run two passes...
		}
		// ...since we need at most two to recalibrate
		if(DEBUG) System.out.println(+numNewEvidence+" new var-> run two passes");
		upwardPassBeliefUpdate(downwardPassBeliefUpdate(_queryTree));
//		runBump();
		if(DEBUG) {
			if(DEBUG) System.out.println("\n\n******\nis tree calibrated?\n\n");
			if(DEBUG) System.out.println(isCalibrated());
//			if(DEBUG) System.out.println(_tree.getLongInfo());
		}
		else {
			isCalibrated();
		}
	}
	/**
	 * Get a query result given a LHS of a query.
	 * At each index of the input lists, we have variable = value pairs.
	 * Note that to keep the format consistent, 
	 * we pass NO_EVIDENCE if no evidence specified.
	 * @param vars
	 * @param values
	 * @return the factor result, or null if query is out of clique inference
	 * @throws FactorIndexException 
	 */
	public Factor getQueryResult(ArrayList<Integer> vars, ArrayList<Integer> values) 
			throws FactorIndexException {
		Vertex target = findVertexInTree(_queryTree, vars);
		if(target == null) {
			System.err.println("uh oh! we can't do out-of-clique inference!");
			return null;
		}
		if(vars.size() != values.size()) {
			System.err.println("uh oh! query vars size must equal query values size");
			return null;
		}
		if(DEBUG) System.out.println("at clique:"+target);
		// check the LHS for evidence...
		ArrayList<Integer> eVars = new ArrayList<Integer>(), 
				eValues = new ArrayList<Integer>();
		for(int i = 0; i < vars.size(); i++) {
			int var = vars.get(i), value = values.get(i);
			if(value != QueryProcessor.NO_EVIDENCE) {
				eVars.add(var);
				eValues.add(value);
			}
		}


		Factor f = new Factor(target);
		if(DEBUG) System.out.println("found clique:\n"+f);
		// marginalize out variables not in the query's LHS
		ArrayList<Integer> diff = f.difference(vars);
		if(DEBUG) System.out.println("marginalize out: "+Factor.variableIndicesToNames(diff));
		f = f.marginalize(diff);
		if(DEBUG) System.out.println("after marginalizing:\n"+f);
		f.normalize();
		if(DEBUG) System.out.println("after normalizing:\n"+f); 
		// and then reduce using the evidence given
		if(DEBUG) System.out.println("Reducing:  "+ Factor.variableIndicesToNames(eVars)+"  "+Factor.valueIndiciesToNames(eVars, eValues));
		f = f.reduce(eVars, eValues);
		if(DEBUG) System.out.println("after reduce:\n"+f);
		return f;
	}
	/**
	 * Get a query result given a LHS of a query.
	 * At each index of the input lists, we have variable = value pairs.
	 * Note that to keep the format consistent, 
	 * we pass NO_EVIDENCE if no evidence specified.
	 * @param vars
	 * @param values
	 * @return the factor result, or null if query is out of clique inference
	 * @throws FactorIndexException 
	 */
	public Factor getQueryResultMaxProduct(ArrayList<Integer> vars, ArrayList<Integer> values) 
			throws FactorIndexException {
		// for each variable find a clique and take the max marginal
		for(int i = 0; i < vars.size(); i++) {
//			System.out.printf("var:%d aka %s\n", i, Factor.variableIndicesToNames(i));
			Vertex target = findVertexInTree(_queryTree, vars.get(i));
//			System.out.println("at clique:"+target);
			// marginalize out all variables not current
			ArrayList<Integer> diff = new ArrayList<Integer>();
			diff.add(i);
			Factor f = target.maxMarginalize(target.difference(diff));
//			System.out.println("have factor:\n"+f);
			// max marginal of this:
			diff.clear();
			diff.add(i);
			Factor g = f.maxMarginalize(diff);
			Double max = 0.0; 
			int max_index = -1;
			for(int idx = 0; idx<f.data.size(); idx++){
				if ( Math.exp(f.data.get(idx)) > max){
					max_index = idx;
					max = f.data.get(idx);
				}
			}
			if( max_index != -1){
				boolean printed = false;
				ArrayList<Integer> max_values = f.valuesFromIndex(max_index);
				for(int m = 0; m < f._variables.size(); m++){
					if(!printed) {
						System.out.printf("Best value for ");
						printed = true;
					}
					System.out.printf("%s = %s -> %s\n",Factor.getVariableName(f._variables.get(m))
							,Factor.getVariableName(f._variables.get(m),
									max_values.get(m)), g);
				}	
			}
		}
		return null;
	}
	/**
	 * Reads in the tree from the clique file.
	 * Builds the tree
	 * @param cliqueTreeFilename
	 * @throws IOException
	 * @throws NumberFormatException
	 */
	public void readCliqueTreeFile(String cliqueTreeFilename) 
			throws IOException, NumberFormatException {
		if(DEBUG) System.out.println("reading clique tree from:"+cliqueTreeFilename);
		BufferedReader br = new BufferedReader(new FileReader(cliqueTreeFilename));
		String line;		
		// on the first line is the number of following lines that describe vertices
		int numCliques = -1;
		if((line = br.readLine()) != null){
			numCliques = Integer.valueOf(line);
		}else{
			br.close();
			throw new IOException();
		}
		//num Cliques
		if(numCliques<0) {
			br.close();
			throw new NumberFormatException();
		}
		//cliques
		for(int i = 0;i<numCliques;i++){
			if((line = br.readLine()) != null){
				String[] containedVars = line.split(",");
//				if(DEBUG) System.out.println("Adding vertex: " + Factor.variableNamesToIndicies(containedVars) );
				Vertex v = new Vertex(containedVars);
				_tree.addVertex(v);
			}else{
				br.close();
				throw new IOException("inconsistent network file.");
			}
		}
		while ((line = br.readLine()) != null) {
			// edges
			String[] tokenized = line.split(" ");
			
			String[] left_variables = tokenized[0].split(",");
			String[] right_variables = tokenized[2].split(",");
			
			ArrayList<Integer> left = Factor.variableNamesToIndicies(left_variables);
			ArrayList<Integer> right = Factor.variableNamesToIndicies(right_variables);
			
			Collections.sort(left);
			Collections.sort(right);
			
			_tree.addEdge(new Edge(
					_tree._vertices.get(left.toString()), 
					_tree._vertices.get(right.toString())));
		}
//		if(DEBUG) System.out.println("\nclique tree structure:\n"+_tree.toString());
		br.close();
	}
	/**
	 * Read in the network file, creating factors part A
	 * @param networkFilename
	 * @throws IOException
	 * @throws NumberFormatException
	 */
	public void readNetworkFile(String networkFilename)
		throws IOException, NumberFormatException {
		if(DEBUG) System.out.println("reading network file from:"+networkFilename);
		BufferedReader br = new BufferedReader(new FileReader(networkFilename));
		String line;
		// on the first line is the number of following lines that describe vertices
		int numVariables = -1;
		if((line = br.readLine()) != null){
			numVariables = Integer.valueOf(line);
		}else{
			br.close();
			throw new IOException();
		}
		if(numVariables<0) {
			br.close();
			throw new NumberFormatException();
		}
		for(int i = 0;i<numVariables;i++){
			if((line = br.readLine()) != null){
				String[] tokenized = line.split(" ");
				String variableName = tokenized[0];
				tokenized = tokenized[1].split(",");//values
//				System.out.printf("var:%s values:",variableName);
//				for(int q = 0; q < tokenized.length; q++) {
//					System.out.printf("%s ", tokenized[q]);
//				}
//				System.out.println();
				Factor.addVariable(variableName, new ArrayList<String>(Arrays.asList(tokenized)));
			}else{
				br.close();
				throw new IOException("inconsistent network file.");
			}
		}
		br.close();
	}
	/**
	 * Read in CPD from a file, creating our factors part B,
	 *  and calculating initial beliefs
	 * @param cpdFilename
	 * @throws IOException
	 * @throws ArrayIndexOutOfBoundsException
	 * @throws IllegalArgumentException
	 * @throws FactorException
	 */
	public void readCPDFile(String cpdFilename) 
			throws IOException, ArrayIndexOutOfBoundsException,
			IllegalArgumentException, FactorException {
		if(DEBUG) System.out.println("reading cpd from:"+cpdFilename);
		BufferedReader br = new BufferedReader(new FileReader(cpdFilename));
		String line;
		
		if(DEBUG) System.out.println("all factor variable info:");
		if(DEBUG) System.out.println(Factor.variableInfo());
		
		HashMap<String,Factor> initialFactors = new HashMap<String,Factor>();
		
		while ((line = br.readLine()) != null) {
//			System.out.println("\nline:"+line);
			String[] tokenized = line.split(" |,");
			ArrayList<String> variables = new ArrayList<String>();
			ArrayList<String> var_value = new ArrayList<String>();
			for(int i = 0; i<tokenized.length-1; i++){
				//System.out.println(tokenized[i]);
				String[] var_pair = tokenized[i].split("=");
				variables.add(var_pair[0]);
				var_value.add(var_pair[1]);
			}
			//System.out.println(tokenized[tokenized.length-1]);
			double prob = Double.valueOf(tokenized[tokenized.length-1]);
			//Put into appropriate clique
			//if(DEBUG) System.out.println(variables);
			//System.out.println(variables+" "+var_value+" "+ prob);
			
			//System.out.println(Factor.variableNamesToIndicies(variables).toString());
			
			ArrayList<Integer> key = Factor.variableNamesToIndicies(variables);
			ArrayList<Integer> val_indicies = Factor.valueNamesToIndicies(variables, var_value);
			// hacky preserving sort
			Map<Integer,Integer> preserveSort = new TreeMap<Integer,Integer>();
			for(int i = 0; i < key.size(); i++) {
				preserveSort.put(key.get(i), val_indicies.get(i));
			}			
			Collections.sort(key);
			// and restore sort of values
			val_indicies.clear();
			for(int k : key) {
				val_indicies.add(preserveSort.get(k));
			}
//			System.out.println(variables+" "+var_value+" "+ prob + "\nkey: "+key.toString()+ "  vals: "+val_indicies.toString());
			//System.out.println(initialFactors.keySet().toString());
			
			if(initialFactors.containsKey(key.toString())){ //already has this factor
				//System.out.println("Adding to previously created factor.");
				initialFactors.get(key.toString()).putProbByValues(val_indicies, prob);
				
			}else{// new factor
				//System.out.println("Creating new factor.");
				Factor newFac = new Factor(key);
				newFac.putProbByValues(val_indicies, prob);
				initialFactors.put(key.toString(), newFac);
			}
		}		
		
		
//		if(DEBUG){
//			System.out.println("==initialFactors==");
//			for(String s:initialFactors.keySet()){
//				System.out.println(Factor.variableIndicesToNames(initialFactors.get(s)._variables));
//			}
//			
//			System.out.println("==Cliques==");
//			for(String s:_tree._vertices.keySet()){
//				System.out.println(Factor.variableIndicesToNames(_tree._vertices.get(s)._variables));
//			}
//		}
		
		for(String initFactors:initialFactors.keySet()){
			ArrayList<Integer> vars = initialFactors.get(initFactors)._variables;
			
			//if(DEBUG) System.out.println("Initial Factor: " + initialFactors.get(initFactors));
			
			boolean foundSuperset = false;
			for(String cliquesKeys:_tree._vertices.keySet()){
				if(_tree._vertices.get(cliquesKeys).contains(vars)){//we're a subset
					//if(DEBUG) System.out.println(initFactors + " is a subset of "+cliquesKeys );
					_tree._vertices.get(cliquesKeys).setFactorData(_tree._vertices.get(cliquesKeys).product(initialFactors.get(initFactors)));
					foundSuperset = true;
					break;
				}
			}
			if(!foundSuperset)
				if(DEBUG) System.err.println("Hmmm... " + Factor.variableIndicesToNames(initialFactors.get(initFactors)._variables) + " has no supersets");
			//System.out.println("initialFact "+ initialFactors.get(s));
			
		}
		br.close();
		
		if(DEBUG){
			System.out.println("==initialBeliefs==");
			for(String cliquesKeys:_tree._vertices.keySet()){
				System.out.println(_tree._vertices.get(cliquesKeys).getLongInfo());
			}
		}
	}
	public static void main(String[] args) {
		String[] newArgs = {
				"/home/mcs/Dropbox/mlcd/assignment2/hw2-files/network-10.8.txt",
				"/home/mcs/Dropbox/mlcd/assignment2/hw2-files/cpd-10.8.txt",
				"/home/mcs/Dropbox/mlcd/assignment2/hw2-files/cliquetree-10.8.txt",
		};
		newArgs = args;
		Bump b = new Bump();
		// calibrate on our tree
			try {
				for(int i = 0; i < newArgs.length; i++) {
					System.out.printf("argument i:%d %s\n", i, newArgs[i]);
				}
				b.init(newArgs[0],newArgs[2],newArgs[1]);
				System.out.println("yay initialized!");
				if(b.runBump());
					System.out.println("yay calibrated!");
			} catch (ArrayIndexOutOfBoundsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (FactorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
}
