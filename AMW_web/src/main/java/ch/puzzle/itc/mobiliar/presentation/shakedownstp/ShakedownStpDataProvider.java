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

package ch.puzzle.itc.mobiliar.presentation.shakedownstp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.inject.Inject;

import org.richfaces.component.SequenceIterationStatus;

import ch.puzzle.itc.mobiliar.business.shakedown.control.ShakedownStpService;
import ch.puzzle.itc.mobiliar.business.shakedown.entity.ShakedownStpEntity;
import ch.puzzle.itc.mobiliar.common.exception.ElementAlreadyExistsException;
import ch.puzzle.itc.mobiliar.common.exception.NotAuthorizedException;
import ch.puzzle.itc.mobiliar.common.exception.StpNotFoundException;
import ch.puzzle.itc.mobiliar.common.exception.TemplateNotDeletableException;
import ch.puzzle.itc.mobiliar.common.util.ConfigurationService;
import ch.puzzle.itc.mobiliar.common.util.ConfigKey;
import ch.puzzle.itc.mobiliar.presentation.CompositeBackingBean;
import ch.puzzle.itc.mobiliar.presentation.util.GlobalMessageAppender;

@CompositeBackingBean
public class ShakedownStpDataProvider implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	ShakedownStpService shakedownStpService;

	private List<ShakedownStpEntity> stp;
	private String newSTPName;
	private String newSTPVersion;
	private Integer selectedSTPId;
	private ShakedownStpEntity selectedSTP;
	private String oldSelectedSTPName;
	private String fileName;
	private List<String> str;

	@PostConstruct
	protected void initView() {
		loadStps();
	}

	private void loadStps() {
		stp = getSTPList();
		setDefaultCurrentSTPEntity();
	}

	public void clearArguments(){
		if(args!=null) {
			args.clear();
		}
		if(stpArguments!=null) {
			stpArguments.clear();
		}
	}

	private void setDefaultCurrentSTPEntity() {
		for (ShakedownStpEntity s : getStp()) {
			setSelectedSTP(s);
			break;
		}
	}

	// STP list in template popupPanel testing mode
	public List<String> getSTR() {
		return str;
	}

	private void clearInput() {
		newSTPName = null;
		newSTPVersion = null;
	}

	public void createNewSTP() {
		addArgumentsToSTP();
		if(stpArguments==null) {
			stpArguments = new ArrayList<String>();
		}
		stpArguments.clear();
		for(STPArgument arg: args){
			if(arg!=null) {
				stpArguments.add(arg.argumentValue);
			}
		}
		createNewSTP(newSTPName, newSTPVersion, stpArguments);
		clearInput();
		loadStps();
		args.clear();
		stpArguments.clear();
	}

	private List<STPArgument> args;
	private List<String> stpArguments;

	public void addArgumentsToSTP() {
		if (args == null) {
			args = new ArrayList<STPArgument>();
		}
		STPArgument arg = new STPArgument();
		args.add(arg);
		if (stpArguments == null) {
			stpArguments = new ArrayList<String>();
		}
		for (STPArgument a : args) {
			if (a.getArgumentValue() != null) {
				stpArguments.add(a.getArgumentValue());
			}
		}
	}

	public void removeSelectedArguments(SequenceIterationStatus argumentId) {
		int index = argumentId.getIndex();
		args.remove(index);
	}

	public class STPArgument {
		private String argumentValue;

		public String getArgumentValue() {
			return argumentValue;
		}

		public void setArgumentValue(String argumentValue) {
			this.argumentValue = argumentValue;
		}
	}

	public List<ShakedownStpEntity> getStp() {
		if (stp == null) {
			stp = new ArrayList<ShakedownStpEntity>();
		}
		return stp;
	}

	public void deleteSTP() throws TemplateNotDeletableException {
		deleteSTP(currentSTPSelected().getId());
		loadStps();
	}

	public ShakedownStpEntity currentSTPSelected() {
		for (ShakedownStpEntity s : getStp()) {
			if (s.getId().equals(selectedSTPId)) {
				setSelectedSTP(s);
				return s;
			}
		}
		return null;
	}

	public void editSTP() {
		if(stpArguments==null) {
			stpArguments = new ArrayList<String>();
		}
		stpArguments.clear();
		for(STPArgument arg: args){
			if(arg!=null) {
				stpArguments.add(arg.argumentValue);
			}
		}
		boolean success = editSTP(selectedSTP, stpArguments);
		if (success && oldSelectedSTPName != null && !oldSelectedSTPName.equals(selectedSTP.getStpName())) {
			String stpRepo = ConfigurationService.getProperty(ConfigKey.STM_REPO);
			GlobalMessageAppender.addSuccessMessage("Please change " + oldSelectedSTPName + ".zip at " + stpRepo + " to "
					+ selectedSTP.getStpName() + ".zip !");
		}
		loadStps();
	}

	public Integer getSelectedSTPId() {
		return selectedSTPId;
	}

	public void setSelectedSTPId(Integer selectedSTPId) {
		this.selectedSTPId = selectedSTPId;
	}

	public String getNewSTPName() {
		return newSTPName;
	}

	public void setNewSTPName(String newSTPName) {
		this.newSTPName = newSTPName;
	}

	public String getNewSTPVersion() {
		return newSTPVersion;
	}

	public void setNewSTPVersion(String newSTPVersion) {
		this.newSTPVersion = newSTPVersion;
	}

	public ShakedownStpEntity getSelectedSTP() {
		return selectedSTP;
	}

	public void setSelectedSTP(ShakedownStpEntity selectedSTP) {
		this.selectedSTP = selectedSTP;
		this.oldSelectedSTPName = selectedSTP.getStpName();
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public List<STPArgument> getArgs() {
		return args;
	}

	public void setArgs(List<STPArgument> args) {
		this.args = args;
	}

	public List<String> getStpArguments() {
		return stpArguments;
	}

	public void setStpArguments(List<String> stpArguments) {
		this.stpArguments = stpArguments;
	}

	public List<STPArgument> createTempArgumentToEditPopupPanel() {
		if (stpArguments == null) {
			stpArguments = new ArrayList<String>();
		} else {
			stpArguments.clear();
		}
		if(args == null) {
			args = new ArrayList<STPArgument>();
		} else {
			args.clear();
		}
		ShakedownStpEntity selectedSTP = currentSTPSelected();
		if(selectedSTP!=null){
			if (selectedSTP.getComaSeperatedParameters() != null) {
				String[] splits = selectedSTP.getComaSeperatedParameters().split(",");
				for (String s : splits) {
					stpArguments.add(s);
					STPArgument a = new STPArgument();
					a.setArgumentValue(s);
					args.add(a);
				}
				return args;
			}
		}
		args.clear();
		return args;
	}

	private List<ShakedownStpEntity> getSTPList(){
		return shakedownStpService.getSTPsWithoutSTS();
	}

	private boolean createNewSTP(String newSTPName, String newSTPVersion, List<String> args) {
		boolean isSucessfully = false;
		String message;
		try {
			if(newSTPName == null || newSTPName.isEmpty()){
				message = "The name of STP must not be empty";
				GlobalMessageAppender.addErrorMessage(message);
			}else if(newSTPVersion== null || newSTPVersion.isEmpty()){
				message = "The version of STP must not be empty";
				GlobalMessageAppender.addErrorMessage(message);
			}else{
				try{
					shakedownStpService.createNewSTP(newSTPName,newSTPVersion,args);
					message = "STP " + newSTPName + " sucessfully created";
					GlobalMessageAppender.addSuccessMessage(message);
					isSucessfully = true;
				}catch(EJBException e){
					if(e.getCause() instanceof NotAuthorizedException) {
						GlobalMessageAppender.addErrorMessage(e.getCause().getMessage());
					} else {
						throw e;
					}
				}
			}
		} catch (ElementAlreadyExistsException e) {
			ElementAlreadyExistsException ex = e;
			String errorMessage = "";
			if(ex.getExistingObjectClass() == ShakedownStpEntity.class){
				errorMessage = "An STP with the name " + e.getExistingObjectName() + " already exists.";
			}
			GlobalMessageAppender.addErrorMessage(errorMessage);
		}
		return isSucessfully;
	}

	private boolean deleteSTP(Integer stpId) throws TemplateNotDeletableException {
		boolean isSucessfully = false;
		try {
			if(stpId == null){
				String errorMessage = "No STP selected";
				GlobalMessageAppender.addErrorMessage(errorMessage);
			}else{
				try{
					shakedownStpService.deleteSTPEntity(stpId);
					String message = "STP succesfully deleted";
					GlobalMessageAppender.addSuccessMessage(message);
					isSucessfully = true;
				}catch(EJBException e){
					if(e.getCause() instanceof NotAuthorizedException) {
						GlobalMessageAppender.addErrorMessage(e.getCause().getMessage());
					} else {
						throw e;
					}
				}
			}
		} catch (StpNotFoundException stpe) {
			String errorMessage = "Could not load selected STP for deleted";
			GlobalMessageAppender.addErrorMessage(errorMessage);
		}
		return isSucessfully;
	}

	private boolean editSTP(ShakedownStpEntity stpEntity,List<String> args){
		boolean isSucessfully = false;
		String message;
		if(stpEntity == null ){
			message = "No STP selected";
			GlobalMessageAppender.addErrorMessage(message);
		}else{
			try{
				List<String> notAddedArgs = shakedownStpService.editSTPEntity(stpEntity, args);
				if (notAddedArgs != null) {
					message = "STP " + stpEntity.getStpName() + " sucessfully updated";
					GlobalMessageAppender.addSuccessMessage(message);
					isSucessfully = true;
					if (!notAddedArgs.isEmpty()) {
						String warning = "The following arguments were not added because they already exist: " + notAddedArgs.toString();
						GlobalMessageAppender.addSuccessMessage(warning);
					}
				} else {
					String errorMessage = "STP " + stpEntity.getStpName() + " could not be saved";
					GlobalMessageAppender.addErrorMessage(errorMessage);
				}
			}catch(EJBException e){
				if(e.getCause() instanceof NotAuthorizedException) {
					GlobalMessageAppender.addErrorMessage(e.getCause().getMessage());
				} else {
					throw e;
				}
			}
		}
		return isSucessfully;
	}

}
