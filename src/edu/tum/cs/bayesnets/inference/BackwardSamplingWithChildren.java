package edu.tum.cs.bayesnets.inference;

import java.util.HashMap;
import java.util.HashSet;

import edu.ksu.cis.bnj.ver3.core.BeliefNode;
import edu.ksu.cis.bnj.ver3.core.CPF;
import edu.ksu.cis.bnj.ver3.core.Discrete;
import edu.ksu.cis.bnj.ver3.core.Domain;
import edu.tum.cs.bayesnets.core.BeliefNetworkEx;
import edu.tum.cs.tools.MutableDouble;
import edu.tum.cs.tools.Stopwatch;

public class BackwardSamplingWithChildren extends BackwardSamplingWithPriors {

	protected HashMap<CPF, HashMap<Integer, Double>> probCache;
	protected HashMap<BeliefNode, HashMap<Long, BackSamplingDistribution>> distCache;
	protected Stopwatch probSW, distSW;
	
	public class BackSamplingDistribution extends edu.tum.cs.bayesnets.inference.BackwardSamplingWithPriors.BackSamplingDistribution {
		
		public BackSamplingDistribution(BackwardSamplingWithPriors sampler) {
			super(sampler);			
		}
		
		/**
		 * recursively gets a distribution to backward sample from 
		 * @param i			the node to instantiate next (as an index into the CPF's domain product)
		 * @param addr		the current setting of node indices of the CPF's domain product
		 * @param cpf		the conditional probability function of the node we are backward sampling
		 */
		@Override
		protected void construct(int i, int[] addr, CPF cpf, int[] nodeDomainIndices) {
			BeliefNode[] domProd = cpf.getDomainProduct();
			if(i == addr.length) {
				double child_prob = cpf.getDouble(addr);
				// temporarily set evidence
				boolean[] tempEvidence = new boolean[addr.length];
				for(int k = 1; k < addr.length; k++) {					
					int nodeIdx = sampler.nodeIndices.get(domProd[k]);
					tempEvidence[k] = nodeDomainIndices[nodeIdx] == -1;
					if(tempEvidence[k])						
						nodeDomainIndices[nodeIdx] = addr[k];
				}
				// consider parent configuration
				double parent_prob = 1.0;
				HashSet<BeliefNode> handledChildren = new HashSet<BeliefNode>();
				handledChildren.add(domProd[0]);
				for(int j = 1; j < addr.length; j++) {
					double[] parentPrior = ((BackwardSamplingWithPriors)sampler).priors.get(domProd[j]);
					parent_prob *= parentPrior[addr[j]]; 
					// consider children of parents with evidence					
					// get child probability
					BeliefNode[] children = sampler.bn.bn.getChildren(domProd[j]);
					for(BeliefNode child : children) {
						if(nodeDomainIndices[sampler.getNodeIndex(child)] >= 0 && !handledChildren.contains(child)) {
							CPF childCPF = child.getCPF();
							//getProb(childCPF, 0, new int[childCPF.getDomainProduct().length], nodeDomainIndices, p);
							double p = getProb(childCPF, nodeDomainIndices);
							parent_prob *= p;
							handledChildren.add(child);
						}
					}
				}
				// unset temporary evidence
				for(int k = 1; k < addr.length; k++) {
					if(tempEvidence[k])
						nodeDomainIndices[sampler.nodeIndices.get(domProd[k])] = -1;
				}
				// add to distribution
				double p = child_prob * parent_prob;
				if(p != 0) {
					addValue(p, addr.clone());
					parentProbs.add(parent_prob);
				}
				return;
			}		
			int nodeIdx = sampler.nodeIndices.get(domProd[i]);
			if(nodeDomainIndices[nodeIdx] >= 0) {
				addr[i] = nodeDomainIndices[nodeIdx];
				construct(i+1, addr, cpf, nodeDomainIndices);
			}
			else {
				Discrete dom = (Discrete)domProd[i].getDomain();		
				for(int j = 0; j < dom.getOrder(); j++) {
					addr[i] = j;
					construct(i+1, addr, cpf, nodeDomainIndices);
				}
			}
		}
		
		protected double getProb(CPF cpf, int[] nodeDomainIndices) {
			probSW.start();
			// get the key in the CPF-specific cache
			Double cacheValue = null;
			BeliefNode[] domProd = cpf.getDomainProduct();
			int[] addr = new int[domProd.length];	
			boolean allSet = true;
			int key = 0;
			for(int i = 0; i < addr.length; i++) {
				int idx = nodeDomainIndices[sampler.getNodeIndex(domProd[i])];
				allSet = allSet && idx >= 0; 
				addr[i] = idx;
				key *= cpf._SizeBuffer[i]+1;
				key += idx == -1 ? cpf._SizeBuffer[i] : idx;
			}
			if(allSet) {
				probSW.stop();
				return cpf.getDouble(addr);
			}
			// check if we already have the value in the cache
			HashMap<Integer, Double> cpfCache = probCache.get(cpf);
			if(cpfCache != null) {
				Double value = cacheValue = cpfCache.get(key);
				if(false && value != null) {
					probSW.stop();
					return value;					
				}
			}
			else {
				cpfCache = new HashMap<Integer,Double>();
				probCache.put(cpf, cpfCache);
			}
			// not in the cache, so calculate the value
			MutableDouble p = new MutableDouble(0.0);
			getProb(cpf, 0, addr, nodeDomainIndices, p);
			// store in cache
			cpfCache.put(key, p.value);
			// return value
			if(cacheValue != null && p.value != cacheValue) {
				throw new RuntimeException("cache mismatch");
			}
			probSW.stop();
			return p.value;
		}
		
		/**
		 * gets the probability indicated by the given CPF for the given domain indices, summing over all parents whose values are not set (i.e. set to -1) in nodeDomainIndices;
		 * i.e. computes the probability of the node whose CPF is provided given the evidence set in nodeDomainIndices
		 * @param cpf					the conditional probability function
		 * @param i						index of the next node to instantiate
		 * @param addr					the address (list of node domain indices relevant to the CPF)
		 * @param nodeDomainIndices		evidences (mapping of all nodes in the network to domain indices, -1 for no evidence)
		 * @param ret					variable in which to store the result (initialize to 0.0, because we are summing probability values)
		 */
		protected void getProb(CPF cpf, int i, int[] addr, int[] nodeDomainIndices, MutableDouble ret) {
			BeliefNode[] domProd = cpf.getDomainProduct();
			// if all nodes have been instantiated...
			if(i == addr.length) {
				double p = cpf.getDouble(addr); 
				for(int j = 1; j < addr.length; j++) {
					if(nodeDomainIndices[sampler.getNodeIndex(domProd[j])] == -1); {
						double[] parentPrior = ((BackwardSamplingWithPriors)sampler).priors.get(domProd[j]);
						p *= parentPrior[addr[j]];						
					}
				}
				ret.value += p;
				return;
			}
			// otherwise instantiate the next node
			BeliefNode node = domProd[i];
			int nodeIdx = sampler.getNodeIndex(node);
			// - if we have evidence, use it
			if(nodeDomainIndices[nodeIdx] >= 0) {				
				addr[i] = nodeDomainIndices[nodeIdx];
				getProb(cpf, i+1, addr, nodeDomainIndices, ret);
			}
			// - otherwise sum over all settings
			else {
				Domain dom = node.getDomain();
				for(int j = 0; j < dom.getOrder(); j++) {
					addr[i] = j;
					getProb(cpf, i+1, addr, nodeDomainIndices, ret);
				}
			}
		}
	}
	
	@Override
	protected BackSamplingDistribution getBackSamplingDistribution(BeliefNode node, WeightedSample s) {
		BackSamplingDistribution d;
		HashMap<Long,BackSamplingDistribution> nodeCache = null;
		long key = 0;
		final boolean useCache = true;
		distSW.start();
		
		if(useCache) {
			// calculate key		
			BeliefNode[] domProd = node.getCPF().getDomainProduct();
			// - consider node itself and all parents			
			for(int i = 0; i < domProd.length; i++) {
				BeliefNode n = domProd[i];
				int idx = s.nodeDomainIndices[getNodeIndex(n)];
				int order = n.getDomain().getOrder();
				key *= order + 1;
				key += idx == -1 ? order : idx;
				// - children of parents
				if(i != 0) {
					BeliefNode[] children = bn.bn.getChildren(n);
					for(int j = 0; j < children.length; j++) {
						if(children[j] != node) {
							n = children[j];
							idx = s.nodeDomainIndices[getNodeIndex(n)];
							order = n.getDomain().getOrder();
							key *= order + 1;
							key += idx == -1 ? order : idx;
							// - parents of children						
							BeliefNode[] parentsofchildren = children[j].getCPF().getDomainProduct();
							for(int k = 1; k < parentsofchildren.length; k++) {
								n = parentsofchildren[k];
								idx = s.nodeDomainIndices[getNodeIndex(n)];
								order = n.getDomain().getOrder();
								key *= order + 1;
								key += idx == -1 ? order : idx;
							}
						}
					}
				}
			}
		
			// check if we have a cache value
			nodeCache = distCache.get(node);
			if(nodeCache != null) {
				d = nodeCache.get(key);
				if(d != null)
					return d;
			}
			else {
				nodeCache = new HashMap<Long, BackSamplingDistribution>();
				distCache.put(node, nodeCache);
			}
		}
		
		// obtain new distribution
		d = new BackSamplingDistribution(this);
		d.construct(node, s.nodeDomainIndices);
		
		// store in cache
		if(useCache)
			nodeCache.put(key, d); 
		
		distSW.stop();
		return d;
	}
	
	public BackwardSamplingWithChildren(BeliefNetworkEx bn) {
		super(bn);
	}
	
	@Override
	public void prepareInference(int[] evidenceDomainIndices) {
		probCache = new HashMap<CPF, HashMap<Integer, Double>>();
		distCache = new HashMap<BeliefNode, HashMap<Long, BackSamplingDistribution>>();
		super.prepareInference(evidenceDomainIndices);
	}
	
	public SampledDistribution infer(int[] evidenceDomainIndices) throws Exception {
		probSW = new Stopwatch();
		distSW = new Stopwatch();
		SampledDistribution d = super.infer(evidenceDomainIndices);
		System.out.println("prob time: " + probSW.getElapsedTimeSecs());
		System.out.println("dist time: " + distSW.getElapsedTimeSecs());
		System.out.println();
		return d;
	}
}
