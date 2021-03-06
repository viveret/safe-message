package com.viveret.safemessage.fb;

/**
 * Created by viveret on 1/14/17.
 */

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.util.JsonReader;
import android.util.Log;

import com.viveret.safemessage.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class ProfileFactory {
    private static final String fbEndPoint = "https://graph.facebook.com/v2.6/<USER_ID>?access_token=PAGE_ACCESS_TOKEN";

    @SuppressLint("InlinedApi")
    private static final String[] PROJECTION =
            {
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.LOOKUP_KEY,
                    Build.VERSION.SDK_INT
                            >= Build.VERSION_CODES.HONEYCOMB ?
                            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY :
                            ContactsContract.Contacts.DISPLAY_NAME

            };

    private Context context;

    private Map<String, IProfile> myProfileCache;

    private String myUserId;

    public ProfileFactory(Context theContext) {
        context = theContext;
        myProfileCache = new HashMap<String, IProfile>();
    }


    public IProfile getUsersProfile() {
        return null;
    }

    public IProfile getProfile(final String uid, final String number) {
        IProfile ret = null;

        if (uid != null && myProfileCache.containsKey(uid))
            ret = myProfileCache.get(uid);
        else if (myProfileCache.containsKey(number))
            ret = myProfileCache.get(number);
        else {
            ret = getProfileFromContacts(number);

            if (ret == null) {
                Log.d(Config.LOGTAG, "Creating profile for " + number);
                Profile tmp = new Profile();
                tmp.setNumber(number);
                if (uid != null) {
                    tmp.setUserId(uid);
                }
                ret = tmp;
                myProfileCache.put(tmp.getNumber(), tmp);
            }
//            HttpURLConnection urlConnection = null;
//            try {
//                URL url = new URL(fbEndPoint);
//                urlConnection = (HttpURLConnection) url.openConnection();
//
//                urlConnection.setRequestMethod("GET");
//                urlConnection.setRequestProperty("Accept", "application/json");
//
//
//                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
//                ret = JSON_ToProfile(in);
//            } catch (MalformedURLException e) {
//                Log.e(Config.LOGTAG, e.toString());
//            } catch (ProtocolException e) {
//                Log.e(Config.LOGTAG, e.toString());
//            } catch (IOException e) {
//                Log.e(Config.LOGTAG, e.toString());
//            } finally {
//                urlConnection.disconnect();
//            }
        }
        return ret;
    }

    private IProfile JSON_ToProfile(final InputStream in) {
        JsonReader reader = null;
        try {
            reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Log.e(Config.LOGTAG, e.toString());
            return null;
        }

        Profile p = new Profile();
        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if ("name".equals(name)) {
                    p.setName(reader.nextString());
                } else if ("profile_pic".equals(name)) {
                    p.setProfilePicUrl(reader.nextString());
                } else if ("locale".equals(name)) {
                    p.setLocale(reader.nextString());
                } else if ("timezone".equals(name)) {
                    p.setTimeZone(reader.nextInt());
                } else if ("gender".equals(name)) {
                    p.setGender(reader.nextString());
                }
            }
        } catch (IOException e) {
            Log.e(Config.LOGTAG, e.toString());
            return null;
        }

        return p;
    }

    public boolean contains(String uid) {
        return myProfileCache.containsKey(uid);
    }

    private IProfile getProfileFromContacts(String number) {
        Profile ret = null;

        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String name = number;
        String contactId = name;
        String avatarURI = null;

        ContentResolver contentResolver = context.getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, new String[]{
                BaseColumns._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        }, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                contactId = contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
                name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                number = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.PhoneLookup.NUMBER));
                avatarURI = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));

                ret = new Profile();
                ret.setName(name);
                ret.setUserId(contactId);
                ret.setProfilePicUrl(avatarURI);
                ret.setNumber(number);
                myProfileCache.put(ret.getNumber(), ret);
                Log.v(Config.LOGTAG, "Retrieved contact " + ret.toString());
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        return ret;
    }
}
