/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.service.access.policy.model;

import aQute.bnd.annotation.ProviderType;

import com.liferay.portal.model.PersistedModel;

/**
 * The extended model interface for the SACPEntry service. Represents a row in the &quot;SACPEntry&quot; database table, with each column mapped to a property of this class.
 *
 * @author Brian Wing Shun Chan
 * @see SACPEntryModel
 * @see com.liferay.service.access.policy.model.impl.SACPEntryImpl
 * @see com.liferay.service.access.policy.model.impl.SACPEntryModelImpl
 * @generated
 */
@ProviderType
public interface SACPEntry extends SACPEntryModel, PersistedModel {
	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify this interface directly. Add methods to {@link com.liferay.service.access.policy.model.impl.SACPEntryImpl} and rerun ServiceBuilder to automatically copy the method declarations to this interface.
	 */
	public java.util.List<java.lang.String> getAllowedServiceSignaturesList();
}