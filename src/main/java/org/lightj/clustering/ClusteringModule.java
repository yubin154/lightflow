package org.lightj.clustering;

import org.lightj.RuntimeContext;
import org.lightj.initialization.BaseInitializable;
import org.lightj.initialization.BaseModule;

/**
 * module initializing cluster capability
 * @author biyu
 *
 */
public class ClusteringModule  {

	/** inner module that controls singleton behavior */
	private static ClusteringInnerModule s_Module = null;
	
	/** constructor */
	public ClusteringModule() {
		init();
	}
	
	/** init */
	private synchronized void init() {
		if (s_Module == null) {
			s_Module = new ClusteringInnerModule();
		}
	}
	
	public BaseModule getModule() {
		return s_Module;
	}
	
	public void setClusterName(String clusterName) {
		s_Module.validateForChange();
		s_Module.clusterName = clusterName;
	}
	
	public String getClusterName() {
		return s_Module.clusterName!=null ? s_Module.clusterName : RuntimeContext.getClusterName();
	}
	
	/** inner module does the real init */
	private class ClusteringInnerModule extends BaseModule {

		private String clusterName;
		ClusteringInnerModule() {
			
			super(new BaseModule[] {});

			addInitializable(new BaseInitializable() {
				@Override
				protected void initialize() {
					ClusteringManager.getInstance();
				}

				@Override
				protected void shutdown() {
					ClusteringManager.getInstance().shutdown();
				}

			});

		}
	}
}
