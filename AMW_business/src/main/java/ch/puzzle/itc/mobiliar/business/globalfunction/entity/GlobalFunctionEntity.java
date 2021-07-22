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

package ch.puzzle.itc.mobiliar.business.globalfunction.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.envers.Audited;

import ch.puzzle.itc.mobiliar.business.database.control.Constants;
import javax.validation.constraints.NotEmpty;

@Entity
@Audited
@Table(name = "TAMW_GLOBALFUNCTION")
public class GlobalFunctionEntity {

	@TableGenerator(name = "globalFunctoinIdGen", table = Constants.GENERATORTABLE, pkColumnName = Constants.GENERATORPKCOLUMNNAME, valueColumnName = Constants.GENERATORVALUECOLUMNNAME, pkColumnValue = "globalFunctionId")
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "globalFunctoinIdGen")
	@Id
	@Column(unique = true, nullable = false)
	@Getter
	private Integer id;

	@Getter
	@Column(nullable = false, unique = true)
	@NotEmpty
	private String name;

	@Getter
	@Setter
	@Column(length = 65536)
	@Lob
	private String content;

	public void setName(String name) {
		if (name != null) {
			name = name.trim();
			if (!name.isEmpty()) {
				this.name = name;
			}
		}
	}
}
