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

package ch.puzzle.itc.mobiliar.business.deploymentparameter.entity;

import ch.puzzle.itc.mobiliar.business.database.control.Constants;
import ch.puzzle.itc.mobiliar.business.deploy.entity.DeploymentEntity;
import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotEmpty;

import javax.persistence.*;

@Entity
@Table(name = "TAMW_deployParam")
public class DeploymentParameter {

    @Getter
    @TableGenerator(name = "deployParameterIdGen", table = Constants.GENERATORTABLE, pkColumnName = Constants.GENERATORPKCOLUMNNAME, valueColumnName = Constants.GENERATORVALUECOLUMNNAME, pkColumnValue = "deployParameterId")
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "deployParameterIdGen")
    @Id
    @Column(unique = true, nullable = false)
    private Integer id;

    @Getter
    @Column(unique = true, nullable = false)
    @NotEmpty
    private String key;

    @Getter
    @Setter
    @Column
    private String value;

    @Getter
    @Setter
    @ManyToOne
    private DeploymentEntity deployment;

    protected DeploymentParameter(){}

    public DeploymentParameter(String key, String value){
        if (key != null) {
            key = key.trim();
            if (!key.isEmpty()) {
                this.key = key;
            }
        }
        this.value = value;
    }

    public void setKey(String key) {
        if (key != null) {
            key = key.trim();
            if (!key.isEmpty()) {
                this.key = key;
            }
        }
    }
}
