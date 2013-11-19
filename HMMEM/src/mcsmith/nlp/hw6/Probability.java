package mcsmith.nlp.hw6;

public class Probability {
	public static Probability ONE = new Probability(1), ZERO = new Probability(0), TWO = new Probability(2);
	private double logprob;
	public Probability(double prob) {
		logprob = Math.log(prob);
	}
	public Probability(double prob, boolean inLogSpace) {
		if(!inLogSpace) {
			logprob = Math.log(prob);
		}
		else {
			logprob = prob;
		}
	}
	public Probability(int prob) {
		logprob = Math.log(new Double(prob));
	}
	public Probability(Probability other) {
		logprob = other.logprob;
	}
	public double getLogProb() {
		return logprob;
	}
	/**
	 * Two probabilties are equal if their values are equal
	 */
	@Override
	public boolean equals(Object other) {
		if(other instanceof Probability) {
			Probability p = (Probability) other;
			return this.logprob == p.logprob;
		}
		return false;
	}
	/**
	 * Returns a new probability containing the maximum probability between this and another
	 * @param other
	 * @return the max probability
	 */
	public Probability max(Probability other) {
		if(Math.max(this.logprob, other.logprob) == this.logprob)
			return new Probability(logprob, true);
		else return new Probability(other.logprob, true);
	}
	/**
	 * Returns a new probability containing the product of this probability and another
	 * @param other
	 * @return this multiplied by another
	 */
	public Probability product(Probability other) {
		return new Probability(logprob + other.logprob, true);
	}
	/**
	 * Returns a new probability containing this probability divided by another
	 * @param other
	 * @return this divided by another
	 */
	public Probability divide(Probability other) {
		return new Probability(logprob - other.logprob, true);
	}
	/**
	 * Returns a new probability containing the sum of this probability and another
	 * @param other
	 * @return the sum of this and another
	 */
	public Probability add(Probability other) {
		// this is x, other is y
		double value;
		if(this.logprob == Double.NEGATIVE_INFINITY
				&& other.logprob == Double.NEGATIVE_INFINITY) {
			// if both x and y are -inf (0)
			value = Double.NEGATIVE_INFINITY;
		}
		else if(this.logprob == Double.NEGATIVE_INFINITY) {
			// if x is -inf, y is not -> y + log(1 + exp(x-y)
			value = other.logprob;
		}
		else if(other.logprob == Double.NEGATIVE_INFINITY) {
			// if y is -inf, x is not -> x + log(1 + exp(y-x)
			value = this.logprob;
		}
		else if(this.logprob < other.logprob) {
			// y + log(1 + exp(x-y)
			value = other.logprob + Math.log1p(Math.exp(this.logprob - other.logprob));
		}
		else if(this.logprob >= other.logprob) {
			// x + log(1 + exp(y-x)
			value = this.logprob + Math.log1p(Math.exp(other.logprob - this.logprob));
		}
		else {
			System.out.println("uh oh!! we didn't account for a case in log add");
			System.err.println("uh oh!! we didn't account for a case in log add");
			value = Double.NaN; // try to throw an error
		}
		return new Probability(value, true);
	}
	public String toString() {
		return Double.toString(Math.exp(logprob));
	}
	public static void main(String[] args) {
		Probability three = new Probability(3), four = new Probability(4), 
				tenth = new Probability(0.1), zero = new Probability(0), zeroA = new Probability(0);
		System.out.println("3+4 = "+three.add(four));
		System.out.println("3x4 = "+three.product(four));
		System.out.println("3/4 = "+three.divide(four));
		System.out.println("max of 3 and 4 = "+three.max(four));
		
		System.out.println("3+0.1 = "+three.add(tenth));
		System.out.println("3x0.1 = "+three.product(tenth));
		System.out.println("3/0.1 = "+three.divide(tenth));
		System.out.println("max of 3 and 0.1 = "+three.max(tenth));
		
		System.out.println("0+0 = "+zero.add(zeroA));
		System.out.println("0x0 = "+zero.product(zeroA));
		System.out.println("0/0 = "+zero.divide(zeroA));
		System.out.println("max of 0 and 0 = "+zero.max(zeroA));
		
		System.out.println("3+0 = "+three.add(zero));
		System.out.println("3x0 = "+three.product(zero));
		System.out.println("3/0 = "+three.divide(zero));
		System.out.println("max of 3 and 0 = "+three.max(zero));
		
		System.out.println("3x4x.1 = "+three.product(four).product(tenth));
		System.out.println("3+4x.1 = "+three.add(four.product(tenth)));
		System.out.println("(3+4)x.1 = "+three.add(four).product(tenth)); 
		System.out.println("3x4+.1 = "+three.product(four).add(tenth));
		System.out.println("3x(4+.1) = "+three.product(four.add(tenth)));
	}
}
