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

package com.liferay.knowledge.base.web.portlet;

import com.liferay.knowledge.base.constants.KBArticleConstants;
import com.liferay.knowledge.base.constants.KBFolderConstants;
import com.liferay.knowledge.base.constants.KBPortletKeys;
import com.liferay.knowledge.base.exception.KBArticleImportException;
import com.liferay.knowledge.base.exception.KBTemplateContentException;
import com.liferay.knowledge.base.exception.KBTemplateTitleException;
import com.liferay.knowledge.base.exception.NoSuchArticleException;
import com.liferay.knowledge.base.exception.NoSuchCommentException;
import com.liferay.knowledge.base.exception.NoSuchTemplateException;
import com.liferay.knowledge.base.model.KBArticle;
import com.liferay.knowledge.base.model.KBFolder;
import com.liferay.knowledge.base.model.KBTemplate;
import com.liferay.knowledge.base.web.constants.KBWebKeys;
import com.liferay.portal.kernel.exception.NoSuchSubscriptionException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Release;
import com.liferay.portal.kernel.portlet.PortletURLFactoryUtil;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import java.io.IOException;
import java.io.InputStream;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowStateException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Peter Shin
 * @author Brian Wing Shun Chan
 * @author Eric Min
 */
@Component(
	immediate = true,
	property = {
		"com.liferay.portlet.add-default-resource=true",
		"com.liferay.portlet.css-class-wrapper=knowledge-base-portlet knowledge-base-portlet-admin",
		"com.liferay.portlet.display-category=category.hidden",
		"com.liferay.portlet.header-portlet-css=/admin/css/common.css,/admin/css/main.css",
		"com.liferay.portlet.icon=/icons/admin.png",
		"com.liferay.portlet.preferences-unique-per-layout=false",
		"com.liferay.portlet.scopeable=true",
		"com.liferay.portlet.show-portlet-access-denied=false",
		"javax.portlet.display-name=Knowledge Base",
		"javax.portlet.expiration-cache=0",
		"javax.portlet.init-param.always-send-redirect=true",
		"javax.portlet.init-param.copy-request-parameters=true",
		"javax.portlet.init-param.template-path=/admin/",
		"javax.portlet.init-param.view-template=/admin/view.jsp",
		"javax.portlet.name=" + KBPortletKeys.KNOWLEDGE_BASE_ADMIN,
		"javax.portlet.resource-bundle=content.Language",
		"javax.portlet.security-role-ref=administrator,guest,power-user,user",
		"javax.portlet.supported-public-render-parameter=categoryId",
		"javax.portlet.supported-public-render-parameter=tag",
		"javax.portlet.supports.mime-type=text/html"
	},
	service = Portlet.class
)
public class AdminPortlet extends BaseKBPortlet {

	public void deleteKBArticles(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
			KBWebKeys.THEME_DISPLAY);

		long[] resourcePrimKeys = StringUtil.split(
			ParamUtil.getString(actionRequest, "resourcePrimKeys"), 0L);

		kbArticleService.deleteKBArticles(
			themeDisplay.getScopeGroupId(), resourcePrimKeys);
	}

	public void deleteKBFolder(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws PortalException {

		long kbFolderId = ParamUtil.getLong(actionRequest, "kbFolderId");

		kbFolderService.deleteKBFolder(kbFolderId);
	}

	public void deleteKBTemplate(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		long kbTemplateId = ParamUtil.getLong(actionRequest, "kbTemplateId");

		kbTemplateService.deleteKBTemplate(kbTemplateId);
	}

	public void deleteKBTemplates(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
			KBWebKeys.THEME_DISPLAY);

		long[] kbTemplateIds = StringUtil.split(
			ParamUtil.getString(actionRequest, "kbTemplateIds"), 0L);

		kbTemplateService.deleteKBTemplates(
			themeDisplay.getScopeGroupId(), kbTemplateIds);
	}

	public void importFile(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		InputStream inputStream = null;

		try {
			ThemeDisplay themeDisplay =
				(ThemeDisplay)actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

			UploadPortletRequest uploadPortletRequest =
				PortalUtil.getUploadPortletRequest(actionRequest);

			checkExceededSizeLimit(actionRequest);

			long parentKBFolderId = ParamUtil.getLong(
				uploadPortletRequest, "parentKBFolderId",
				KBFolderConstants.DEFAULT_PARENT_FOLDER_ID);

			String fileName = uploadPortletRequest.getFileName("file");

			if (Validator.isNull(fileName)) {
				throw new KBArticleImportException("File name is null");
			}

			boolean prioritizeByNumericalPrefix = ParamUtil.getBoolean(
				uploadPortletRequest, "prioritizeByNumericalPrefix");

			inputStream = uploadPortletRequest.getFileAsStream("file");

			ServiceContext serviceContext = ServiceContextFactory.getInstance(
				AdminPortlet.class.getName(), actionRequest);

			serviceContext.setGuestPermissions(new String[] {ActionKeys.VIEW});

			int importedKBArticlesCount =
				kbArticleService.addKBArticlesMarkdown(
					themeDisplay.getScopeGroupId(), parentKBFolderId, fileName,
					prioritizeByNumericalPrefix, inputStream, serviceContext);

			SessionMessages.add(
				actionRequest, "importedKBArticlesCount",
				importedKBArticlesCount);
		}
		catch (KBArticleImportException kbaie) {
			SessionErrors.add(actionRequest, kbaie.getClass(), kbaie);
		}
		finally {
			StreamUtil.cleanUp(inputStream);
		}
	}

	@Override
	public void render(
			RenderRequest renderRequest, RenderResponse renderResponse)
		throws IOException, PortletException {

		try {
			int status = WorkflowConstants.STATUS_ANY;

			renderRequest.setAttribute(KBWebKeys.KNOWLEDGE_BASE_STATUS, status);

			KBArticle kbArticle = null;

			long kbArticleClassNameId = PortalUtil.getClassNameId(
				KBArticleConstants.getClassName());

			long resourceClassNameId = ParamUtil.getLong(
				renderRequest, "resourceClassNameId", kbArticleClassNameId);
			long resourcePrimKey = ParamUtil.getLong(
				renderRequest, "resourcePrimKey");

			if ((resourcePrimKey > 0) &&
				(resourceClassNameId == kbArticleClassNameId)) {

				kbArticle = kbArticleService.getLatestKBArticle(
					resourcePrimKey, status);
			}

			renderRequest.setAttribute(
				KBWebKeys.KNOWLEDGE_BASE_KB_ARTICLE, kbArticle);

			KBTemplate kbTemplate = null;

			long kbTemplateId = ParamUtil.getLong(
				renderRequest, "kbTemplateId");

			if (kbTemplateId > 0) {
				kbTemplate = kbTemplateService.getKBTemplate(kbTemplateId);
			}

			renderRequest.setAttribute(
				KBWebKeys.KNOWLEDGE_BASE_KB_TEMPLATE, kbTemplate);
		}
		catch (Exception e) {
			if (e instanceof NoSuchArticleException ||
				e instanceof NoSuchTemplateException ||
				e instanceof PrincipalException) {

				SessionErrors.add(renderRequest, e.getClass());
			}
			else {
				throw new PortletException(e);
			}
		}

		super.render(renderRequest, renderResponse);
	}

	public void subscribeGroupKBArticles(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
			KBWebKeys.THEME_DISPLAY);

		String portletId = PortalUtil.getPortletId(actionRequest);

		kbArticleService.subscribeGroupKBArticles(
			themeDisplay.getScopeGroupId(), portletId);
	}

	public void unsubscribeGroupKBArticles(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
			KBWebKeys.THEME_DISPLAY);

		String portletId = PortalUtil.getPortletId(actionRequest);

		kbArticleService.unsubscribeGroupKBArticles(
			themeDisplay.getScopeGroupId(), portletId);
	}

	public void updateKBArticlesPriorities(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
			KBWebKeys.THEME_DISPLAY);

		Enumeration<String> enu = actionRequest.getParameterNames();

		Map<Long, Double> resourcePrimKeyToPriorityMap = new HashMap<>();

		while (enu.hasMoreElements()) {
			String name = enu.nextElement();

			if (!name.startsWith("priority")) {
				continue;
			}

			double priority = ParamUtil.getDouble(actionRequest, name);

			long resourcePrimKey = GetterUtil.getLong(name.substring(8));

			resourcePrimKeyToPriorityMap.put(resourcePrimKey, priority);
		}

		kbArticleService.updateKBArticlesPriorities(
			themeDisplay.getScopeGroupId(), resourcePrimKeyToPriorityMap);
	}

	public void updateKBFolder(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws PortalException {

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
			KBWebKeys.THEME_DISPLAY);

		String cmd = ParamUtil.getString(actionRequest, Constants.CMD);

		long kbFolderId = ParamUtil.getLong(actionRequest, "kbFolderId");

		long parentResourceClassNameId = ParamUtil.getLong(
			actionRequest, "parentResourceClassNameId");
		long parentResourcePrimKey = ParamUtil.getLong(
			actionRequest, "parentResourcePrimKey");
		String name = ParamUtil.getString(actionRequest, "name");
		String description = ParamUtil.getString(actionRequest, "description");

		ServiceContext serviceContext = ServiceContextFactory.getInstance(
			KBFolder.class.getName(), actionRequest);

		if (cmd.equals(Constants.ADD)) {
			kbFolderService.addKBFolder(
				themeDisplay.getScopeGroupId(), parentResourceClassNameId,
				parentResourcePrimKey, name, description, serviceContext);
		}
		else if (cmd.equals(Constants.UPDATE)) {
			kbFolderService.updateKBFolder(
				parentResourceClassNameId, parentResourcePrimKey, kbFolderId,
				name, description);
		}
	}

	public void updateKBTemplate(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		String portletId = PortalUtil.getPortletId(actionRequest);

		String cmd = ParamUtil.getString(actionRequest, Constants.CMD);

		long kbTemplateId = ParamUtil.getLong(actionRequest, "kbTemplateId");

		String title = ParamUtil.getString(actionRequest, "title");
		String content = ParamUtil.getString(actionRequest, "content");

		ServiceContext serviceContext = ServiceContextFactory.getInstance(
			KBTemplate.class.getName(), actionRequest);

		if (cmd.equals(Constants.ADD)) {
			kbTemplateService.addKBTemplate(
				portletId, title, content, serviceContext);
		}
		else if (cmd.equals(Constants.UPDATE)) {
			kbTemplateService.updateKBTemplate(
				kbTemplateId, title, content, serviceContext);
		}
	}

	@Override
	protected String buildEditURL(
			ActionRequest actionRequest, ActionResponse actionResponse,
			KBArticle kbArticle)
		throws PortalException {

		try {
			ThemeDisplay themeDisplay =
				(ThemeDisplay)actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

			PortletURL portletURL = PortletURLFactoryUtil.create(
				actionRequest, KBPortletKeys.KNOWLEDGE_BASE_ADMIN,
				themeDisplay.getPlid(), PortletRequest.RENDER_PHASE);

			portletURL.setParameter(
				"mvcPath", templatePath + "edit_article.jsp");
			portletURL.setParameter(
				"redirect", getRedirect(actionRequest, actionResponse));
			portletURL.setParameter(
				"resourcePrimKey",
				String.valueOf(kbArticle.getResourcePrimKey()));
			portletURL.setWindowState(actionRequest.getWindowState());

			return portletURL.toString();
		}
		catch (WindowStateException wse) {
			throw new PortalException(wse);
		}
	}

	@Override
	protected void doDispatch(
			RenderRequest renderRequest, RenderResponse renderResponse)
		throws IOException, PortletException {

		if (SessionErrors.contains(
				renderRequest, NoSuchArticleException.class.getName()) ||
			SessionErrors.contains(
				renderRequest, NoSuchCommentException.class.getName()) ||
			SessionErrors.contains(
				renderRequest, NoSuchSubscriptionException.class.getName()) ||
			SessionErrors.contains(
				renderRequest, NoSuchTemplateException.class.getName()) ||
			SessionErrors.contains(
				renderRequest, PrincipalException.getNestedClasses())) {

			include(templatePath + "error.jsp", renderRequest, renderResponse);
		}
		else {
			super.doDispatch(renderRequest, renderResponse);
		}
	}

	@Override
	protected boolean isSessionErrorException(Throwable cause) {
		if (cause instanceof KBArticleImportException ||
			cause instanceof KBTemplateContentException ||
			cause instanceof KBTemplateTitleException ||
			cause instanceof NoSuchTemplateException ||
			super.isSessionErrorException(cause)) {

			return true;
		}

		return false;
	}

	@Reference(
		target = "(&(release.bundle.symbolic.name=com.liferay.knowledge.base.web)(release.schema.version=1.0.0))",
		unbind = "-"
	)
	protected void setRelease(Release release) {
	}

}