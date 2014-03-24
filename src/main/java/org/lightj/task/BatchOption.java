package org.lightj.task;

import akka.actor.Actor;
import akka.actor.UntypedActorFactory;

/**
 * batch option
 * @author biyu
 *
 */
public class BatchOption {
	
	public interface RateSettingMessage {
		public int getMaxRate();
	}
	
	/** batch strategy */
	public static enum Strategy {
		UNLIMITED(new UntypedActorFactory() {
				private static final long serialVersionUID = 1L;

				@Override
				public Actor create() throws Exception {
					return new UnlimitedStrategy();
				}

			}),
		MAX_CONCURRENT_RATE_SLIDING(new UntypedActorFactory() {
			private static final long serialVersionUID = 1L;

			@Override
			public Actor create() throws Exception {
				return new MaxConcurrentStrategy();
			}

		});
		private final UntypedActorFactory strategyActorFactory;
		Strategy(UntypedActorFactory strategyActorFactory) {
			this.strategyActorFactory = strategyActorFactory;
		}
		public UntypedActorFactory getStrategyActorFactory() {
			return strategyActorFactory;
		}
		
	};
	
	private int concurrentRate;
	private Strategy strategy;
	
	public BatchOption() {}
	
	public BatchOption(int concurrentRate, Strategy strategy) {
		this.concurrentRate = concurrentRate;
		this.strategy = strategy;
	}
	
	public void setConcurrentRate(int concurrentRate) {
		this.concurrentRate = concurrentRate;
	}

	public void setStrategy(Strategy strategy) {
		this.strategy = strategy;
	}

	public int getConcurrentRate() {
		return concurrentRate;
	}
	public Strategy getStrategy() {
		return strategy;
	}

}
