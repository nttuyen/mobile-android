package org.exoplatform.controller.home;

import greendroid.widget.LoaderActionBarItem;

import java.net.MalformedURLException;
import java.net.URL;

import org.exoplatform.singleton.AccountSetting;
import org.exoplatform.singleton.SocialServiceHelper;
import org.exoplatform.social.client.api.ClientServiceFactory;
import org.exoplatform.social.client.api.SocialClientContext;
import org.exoplatform.social.client.api.SocialClientLibException;
import org.exoplatform.social.client.api.model.RestActivity;
import org.exoplatform.social.client.api.model.RestIdentity;
import org.exoplatform.social.client.api.model.RestProfile;
import org.exoplatform.social.client.api.service.ActivityService;
import org.exoplatform.social.client.api.service.IdentityService;
import org.exoplatform.social.client.api.service.VersionService;
import org.exoplatform.social.client.core.ClientServiceFactoryHelper;
import org.exoplatform.ui.HomeActivity;
import org.exoplatform.utils.ExoConstants;
import org.exoplatform.utils.SocialActivityUtil;
import org.exoplatform.widget.WarningDialog;

import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;

import com.cyrilmottier.android.greendroid.R;

public class SocialServiceLoadTask extends AsyncTask<Void, Void, String[]> {
  private Context                       mContext;

  private String                        okString;

  private String                        titleString;

  private String                        contentString;

  private HomeController                homeController;

  private ActivityService<RestActivity> activityService;

  private IdentityService<RestIdentity> identityService;

  private String                        userIdentity;

  private LoaderActionBarItem           loaderItem;

  public SocialServiceLoadTask(Context context,
                               HomeController controller,
                               LoaderActionBarItem loader) {
    mContext = context;
    homeController = controller;
    loaderItem = loader;
    changeLanguage();
  }

  private void changeLanguage() {
    Resources resource = mContext.getResources();
    okString = resource.getString(R.string.OK);
    titleString = resource.getString(R.string.Warning);
    contentString = resource.getString(R.string.LoadingDataError);

  }

  @Override
  public void onPreExecute() {
    loaderItem.setLoading(true);

  }

  @SuppressWarnings({ "deprecation", "unchecked" })
  @Override
  public String[] doInBackground(Void... params) {
    try {
      String userName = AccountSetting.getInstance().getUsername();
      String password = AccountSetting.getInstance().getPassword();
      URL url = new URL(SocialActivityUtil.getDomain());
      SocialClientContext.setProtocol(url.getProtocol());
      SocialClientContext.setHost(url.getHost());
      SocialClientContext.setPort(url.getPort());
      SocialClientContext.setPortalContainerName(ExoConstants.ACTIVITY_PORTAL_CONTAINER);
      SocialClientContext.setRestContextName(ExoConstants.ACTIVITY_REST_CONTEXT);
      SocialClientContext.setUsername(userName);
      SocialClientContext.setPassword(password);

      ClientServiceFactory clientServiceFactory = ClientServiceFactoryHelper.getClientServiceFactory();
      VersionService versionService = clientServiceFactory.createVersionService();
      SocialClientContext.setRestVersion(versionService.getLatest());
      clientServiceFactory = ClientServiceFactoryHelper.getClientServiceFactory();

      activityService = clientServiceFactory.createActivityService();
      identityService = clientServiceFactory.createIdentityService();
      userIdentity = identityService.getIdentityId(ExoConstants.ACTIVITY_ORGANIZATION, userName);
      RestIdentity restIdent = identityService.getIdentity(ExoConstants.ACTIVITY_ORGANIZATION,
                                                           userName);
      RestProfile profile = restIdent.getProfile();
      String[] profileArray = new String[2];
      profileArray[0] = profile.getAvatarUrl();
      profileArray[1] = profile.getFullName();

      return profileArray;
    } catch (SocialClientLibException e) {
      return null;
    } catch (RuntimeException e) {
      return null;
    } catch (MalformedURLException e) {
      return null;
    }
  }

  @Override
  public void onPostExecute(String[] result) {

    if (result != null) {
      SocialServiceHelper.getInstance().userIdentity = userIdentity;
      SocialServiceHelper.getInstance().activityService = activityService;
      SocialServiceHelper.getInstance().identityService = identityService;
      SocialServiceHelper.getInstance().userProfile = result;
      if (HomeActivity.homeActivity != null) {
        HomeActivity.homeActivity.setProfileInfo(result);
      }

      homeController.onLoad(ExoConstants.NUMBER_OF_ACTIVITY, loaderItem);

    } else {
      loaderItem.setLoading(false);
      WarningDialog dialog = new WarningDialog(mContext, titleString, contentString, okString);
      dialog.show();
    }
  }
}