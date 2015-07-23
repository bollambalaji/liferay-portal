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

package com.liferay.service.access.policy;

import com.liferay.portal.kernel.security.service.access.policy.ServiceAccessPolicy;
import com.liferay.service.access.policy.model.SACPEntry;

import java.util.List;
import java.util.Locale;

/**
 * @author Mika Koivisto
 */
public class ServiceAccessControlProfileImpl implements ServiceAccessPolicy {

	public ServiceAccessControlProfileImpl(SACPEntry sacpEntry) {
		_sacpEntry = sacpEntry;
	}

	@Override
	public List<String> getAllowedServiceSignaturesList() {
		return _sacpEntry.getAllowedServiceSignaturesList();
	}

	@Override
	public String getName() {
		return _sacpEntry.getName();
	}

	@Override
	public String getTitle(Locale locale) {
		return _sacpEntry.getTitle(locale);
	}

	private final SACPEntry _sacpEntry;

}