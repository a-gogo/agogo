/*
 * AMW - Automated Middleware allows you to manage the configurations of
 * your Java EE applications on an unlimited number of different environments
 * with various versions, including the automated deployment of those apps.
 * Copyright (C) 2013-2016 by Puzzle ITC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ch.puzzle.itc.mobiliar.presentation.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import ch.puzzle.itc.mobiliar.business.domain.commons.CommonDomainService;
import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.ResourceGroupLocator;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroupEntity;
import ch.puzzle.itc.mobiliar.business.server.boundary.ServerView;
import ch.puzzle.itc.mobiliar.business.server.entity.ServerTuple;
import ch.puzzle.itc.mobiliar.business.utils.JpaWildcardConverter;
import ch.puzzle.itc.mobiliar.common.util.ConfigurationService;
import ch.puzzle.itc.mobiliar.common.util.DefaultResourceTypeDefinition;
import ch.puzzle.itc.mobiliar.common.util.ConfigKey;
import ch.puzzle.itc.mobiliar.presentation.ViewBackingBean;
import ch.puzzle.itc.mobiliar.presentation.security.SecurityDataProvider;
import ch.puzzle.itc.mobiliar.presentation.util.NavigationUtils;
import lombok.Getter;
import lombok.Setter;

@Named
@ViewBackingBean
public class ServerListView implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final String FILTER_TOOLTIP = "Use "+JpaWildcardConverter.WILDCARD_CHARACTER+" as wildcard";

	@Inject
	private ServerView serverView;
	@Inject
	private ResourceGroupLocator resourceGroupLocator;
	@Inject
	private CommonDomainService commonServcie;
	@Inject
	private SecurityDataProvider securityDataProvider;
	
	@Inject
	@Getter
	private ServerListFilter serverListFilter;
	
	@Setter
	@Getter
	private List<ServerTuple> servers;
	@Setter
	@Getter
	private List<ResourceGroupEntity> runtimes;
	private List<String> appServers = null;
	
	@Getter
	private String vmDetailUrl = ConfigurationService.getProperty(ConfigKey.VM_DETAIL_URL);
	@Getter
	private String vmUrlParam = ConfigurationService.getProperty(ConfigKey.VM_URL_PARAM);

	@Getter
	private boolean appServerReadPermission;
	@Getter
	private boolean resourceReadPermission;

	// called by preRenderView event
	public void init() {
		//cache permissions in view
		this.appServerReadPermission = securityDataProvider.hasPermissionForResourceType("RESOURCE", "READ", "APPLICATIONSERVER");
		this.resourceReadPermission = securityDataProvider.hasPermission("RESOURCE", "READ");
		if(!FacesContext.getCurrentInstance().isPostback()) {
			if(!serverListFilter.isEmpty() || serverListFilter.isEmptySearch()) {
				servers = serverView.getServers(serverListFilter.getHost(), serverListFilter.getAppServer(), serverListFilter.getRuntime(),
						serverListFilter.getNode(), serverListFilter.getEnvironment(), true);
			}
			loadRuntimes();
		}
	}
	
	// must be an non static method or else JSF wont find it
	public String getFILTER_TOOLTIP() {
		return FILTER_TOOLTIP;
	}

	public void loadRuntimes() {
		runtimes = commonServcie.getRuntimeResourceGroups();
	}
	
	public List<String> getAppServersSuggestions(String prefix){
		if(appServers == null) {
			appServers = new ArrayList<>();
			List<ResourceGroupEntity> appServerGroups = resourceGroupLocator.getGroupsForType(
					DefaultResourceTypeDefinition.APPLICATIONSERVER.name(), 
					null, false, true);
			for(ResourceGroupEntity appServer : appServerGroups) {
				appServers.add(appServer.getName());
			}
		}
		return appServers;
	}
	
	//only used to redirect to new url -> leads to init call
	public String searchServers() {
		if(serverListFilter.isEmpty()) {
			serverListFilter.setEmptySearch(true);
		}
		else {
			serverListFilter.setEmptySearch(false);
		}
		return NavigationUtils.getRefreshOutcomeWithAdditionalParam("includeViewParams=true");
	}

}
