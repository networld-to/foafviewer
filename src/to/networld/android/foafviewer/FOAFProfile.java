package to.networld.android.foafviewer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;

import to.networld.android.foafviewer.model.Agent;
import to.networld.android.foafviewer.model.AgentHandler;
import to.networld.android.foafviewer.model.ImageHelper;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Shows the Profile of an Agent. Please assure that you assign 
 * a serializable object of AgentSerializable with the attribute
 * "agent".
 * 
 * intentObj.putExtra("agent", foafAgent.getSerializableObject());
 * 
 * @author Alex Oberhauser
 *
 */
public class FOAFProfile extends Activity {
	private final Context context = FOAFProfile.this;
	
	private Agent agent = null;
	
	private static final String BOTTOM = "bottom";
	private static final String TOP = "top";
	private static final String ICON = "icon";
	private static final String ARROW = "nextArrow";
	
	private static final String NAME = "Name";
	private static final String BIRTHDAY = "Date of Birth";
	private static final String LOCATION = "Location";
	private static final String PRIVATE_WEBSITE = "Private Website";
	private static final String WEBLOG = "Weblog aka. Blog";
	private static final String OPENID = "OpenID Account";
	private static final String SCHOOL_HOMEPAGE = "Education Institution Website";
	private static final String WORKPLACE_HOMEPAGE = "Workplace Website";
	private static final String MAIL = "E-Mail";
	private static final String SCUBA_CERT = "Scuba Certificate";
	private static final String INTEREST = "Interest";
	private static final String PHONE_NUMBER = "Phone Number";
	private static final String KNOWN_AGENT = "Known Agent";
	
	private ListView list; 
	
	private final Handler guiHandler = new Handler();
	private final Runnable updateProfile = new Runnable() {
		@Override
		public void run() {
			 updateProfileInGUI();
		}
	};
	
	private ArrayList<HashMap<String, String>> profileList;
	

	private void updateProfileInGUI() {
		SimpleAdapter adapterProfileList = new SimpleAdapter(context, profileList, 
				R.layout.list_entry, new String[]{ ICON, TOP, BOTTOM, ARROW },
				new int[] { R.id.icon, R.id.topText, R.id.bottomText, R.id.nextArrow });
		list.setAdapter(adapterProfileList);
	}
	
	private OnItemClickListener listClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> list, View view, int position, long id) {
			HashMap<String, String> entry = profileList.get(position);

			if ( entry.get(TOP).equals(MAIL) ) {
				final Intent eMailIntent = new Intent(android.content.Intent.ACTION_SEND);
				eMailIntent.setType("text/html");
				eMailIntent .putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{ entry.get(BOTTOM) });
				startActivity(eMailIntent);
			} else if ( entry.get(TOP).equals(NAME) ) {
				/**
				 * XXX: Dialog too big for the image.
				 */
				Dialog picDialog = new Dialog(context);
				ImageView portrait = new ImageView(context);
				try {
					Drawable drawable = ImageHelper.getDrawable(agent.getImageURL());
					portrait.setBackgroundDrawable(drawable);
					picDialog.addContentView(portrait, new LinearLayout.LayoutParams(drawable.getMinimumWidth(), drawable.getMinimumHeight()));
				} catch (MalformedURLException e) {
				} catch (IOException e) {}
				picDialog.show();
			} else if ( entry.get(TOP).equals(PHONE_NUMBER) ) {
				final Intent phoneIntent = new Intent(android.content.Intent.ACTION_CALL);
				phoneIntent.setData(Uri.parse("tel:" + entry.get(BOTTOM)));
				startActivity(phoneIntent);
			} else if ( entry.get(TOP).equals(LOCATION) ) {
				final Intent mapIntent = new Intent(android.content.Intent.ACTION_VIEW);
				String location = entry.get(BOTTOM).replace(" ", "+");
				mapIntent.setData(Uri.parse("geo:0,0?q=" + location));
				startActivity(mapIntent);
			} else if ( entry.get(TOP).equals(KNOWN_AGENT) ) {
				final Intent profileIntent = new Intent(FOAFProfile.this, FOAFProfile.class);
				profileIntent.putExtra("agent", AgentHandler.getAgent(entry.get(BOTTOM)).getURI());
				startActivity(profileIntent);
			} else if ( entry.get(TOP).equals(INTEREST) ) {
				/**
				 * TODO: Call here a window (dialog!?!) that displays the interests.
				 */
			} else if ( entry.get(TOP).equals(SCUBA_CERT) ) {
				
			} else if ( entry.get(TOP).equals(PRIVATE_WEBSITE)
					||  entry.get(TOP).equals(WEBLOG)
					||  entry.get(TOP).equals(OPENID)
					||  entry.get(TOP).equals(SCHOOL_HOMEPAGE)
					||  entry.get(TOP).equals(WORKPLACE_HOMEPAGE) ) {
				final Intent websiteIntent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(entry.get(BOTTOM)));
				startActivity(websiteIntent);
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		final ProgressDialog progressDialog = ProgressDialog.show(FOAFProfile.this, null, "Preparing FOAF Profile...", false);
		setContentView(R.layout.profile);
		String agentURL = getIntent().getStringExtra("agent");
		try {
			this.agent = AgentHandler.initAgent(agentURL, this);
		} catch (Exception e) {
			e.printStackTrace();
			new GenericDialog(context, "Error", e.getLocalizedMessage(), R.drawable.error_icon);
		}
			
		this.list = (ListView) findViewById(R.id.profileList);
		this.list.setOnItemClickListener(listClickListener);
		this.profileList = new ArrayList<HashMap<String, String>>();
		
		Thread seeker = new Thread() {
			@Override
			public void run(){
				/**
				 * Name
				 */
				HashMap<String, String> map = new HashMap<String, String>();
				map.put(ICON, R.drawable.avatar_icon + "");
				map.put(TOP, NAME);
				map.put(BOTTOM, agent.getName());
				profileList.add(map);
				
				/**
				 * Date of Birth
				 */
				String dateOfBirth = agent.getDateOfBirth();
				if ( dateOfBirth != null ) {
					map = new HashMap<String, String>();
					map.put(ICON, R.drawable.birthday_icon + "");
					map.put(TOP, BIRTHDAY);
					map.put(BOTTOM, dateOfBirth);
					profileList.add(map);
				}
				
				/**
				 * Location
				 */
				Geocoder geocoder = new Geocoder(context, Locale.getDefault());
				try {
					Pair<Double, Double> geoPoint = agent.getLocation();
					Address address = geocoder.getFromLocation(geoPoint.first, geoPoint.second, 1).get(0);
					map = new HashMap<String, String>();
					map.put(ICON, R.drawable.location_icon + "");
					map.put(TOP, LOCATION);
					map.put(BOTTOM, address.getAddressLine(0) + ", " + address.getPostalCode() + " " + address.getLocality());
					profileList.add(map);
				} catch (Exception e) {}
				
				/**
				 * Private Website
				 */
				String website = agent.getWebsite();
				if ( website != null ) {
					map = new HashMap<String, String>();
					map.put(ICON, R.drawable.website_private_icon + "");
					map.put(TOP, PRIVATE_WEBSITE);
					map.put(BOTTOM, website);
					profileList.add(map);
				}
				
				/**
				 * Weblog
				 */
				String weblog = agent.getWeblog();
				if ( weblog != null ) {
					map = new HashMap<String, String>();
					map.put(ICON, R.drawable.weblog_icon + "");
					map.put(TOP, WEBLOG);
					map.put(BOTTOM, weblog);
					profileList.add(map);
				}
				
				/**
				 * OpenID Account
				 */
				String openid = agent.getOpenid();
				if ( openid != null ) {
					map = new HashMap<String, String>();
					map.put(ICON, R.drawable.openid_icon + "");
					map.put(TOP, OPENID);
					map.put(BOTTOM, openid);
					profileList.add(map);
				}
				
				/**
				 * Education Institution Website
				 */
				String schoolHomepage = agent.getSchoolHomepage();
				if ( schoolHomepage != null ) {
					map = new HashMap<String, String>();
					map.put(ICON, R.drawable.learning_icon + "");
					map.put(TOP, SCHOOL_HOMEPAGE);
					map.put(BOTTOM, schoolHomepage);
					profileList.add(map);
				}
				
				/**
				 * Workplace Website
				 */
				String workplaceHomepage = agent.getWorkplaceHomepage();
				if ( workplaceHomepage != null ) {
					map = new HashMap<String, String>();
					map.put(ICON, R.drawable.workplace_icon + "");
					map.put(TOP, WORKPLACE_HOMEPAGE);
					map.put(BOTTOM, workplaceHomepage);
					profileList.add(map);
				}
				
				/**
				 * E-Mails
				 */
				Vector<String> mails = agent.getEMails();
				for (String eMail : mails) {
					map = new HashMap<String, String>();
					map.put(ICON, R.drawable.email_icon + "");
					map.put(TOP, MAIL);
					map.put(BOTTOM, eMail);
					profileList.add(map);
				}
				
				/**
				 * Phone Numbers
				 */
				Vector<String> phoneNumbers = agent.getPhoneNumbers();
				for (String phoneNumber : phoneNumbers) {
					map = new HashMap<String, String>();
					map.put(ICON, R.drawable.tel_icon + "");
					map.put(TOP, PHONE_NUMBER);
					map.put(BOTTOM, phoneNumber);
					profileList.add(map);
				}
				
				/**
				 * TODO: Twitter Account
				 */
				
				/**
				 * TODO: Facebook Account
				 */
				
				/**
				 * Known Agents
				 * TODO: Visualize the known Agents in a separate window.
				 */
				Vector<String> knownAgents = agent.getKnownAgentsNames();
				for (String knownAgent : knownAgents) {
					map = new HashMap<String, String>();
					map.put(ICON, R.drawable.avatar_icon + "");
					map.put(TOP, KNOWN_AGENT);
					map.put(BOTTOM, knownAgent);
					profileList.add(map);
				}
				
				/**
				 * Scuba Dive Certificate
				 */
				String scubaCertificate = agent.getDiveCertificate();
				if ( scubaCertificate != null ) {
					map = new HashMap<String, String>();
					map.put(ICON, R.drawable.diver_icon + "");
					map.put(TOP, SCUBA_CERT);
					map.put(BOTTOM, scubaCertificate);
					profileList.add(map);
				}
				
				/**
				 * Interests
				 * TODO: Visualize the interests in a separate window.
				 */
				map = new HashMap<String, String>();
				map.put(ICON, R.drawable.leisure_icon + "");
				map.put(TOP, INTEREST);
				map.put(BOTTOM, "See the interests of this person.");
				map.put(ARROW, R.drawable.right_arrow_icon + "");
				profileList.add(map);

				/*
				Vector<String> interests = agent.getInterests();
				for (String interest : interests) {
					map = new HashMap<String, String>();
					map.put(ICON, R.drawable.leisure_icon + "");
					map.put(TOP, INTEREST);
					map.put(BOTTOM, interest);
					map.put(ARROW, R.drawable.right_arrow_icon + "");
					profileList.add(map);
				}
				*/
				
				guiHandler.post(updateProfile);
				progressDialog.dismiss();
			}
		};
		seeker.start();
	}

}
