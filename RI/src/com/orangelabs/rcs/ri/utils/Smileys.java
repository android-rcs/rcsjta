/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.orangelabs.rcs.ri.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SimpleAdapter;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.AbstractMessageParser.TrieNode;

/**
 * Resources for smiley parser.
 */
public class Smileys implements AbstractMessageParser.Resources {
    private HashMap<String, Integer> mSmileyToRes = new HashMap<String, Integer>();
    
    private static final int[] DEFAULT_SMILEY_RES_IDS = {
        R.drawable.ri_smiley_happy,                //  0
        R.drawable.ri_smiley_sad,                  //  1
        R.drawable.ri_smiley_winking,              //  2
        R.drawable.ri_smiley_tongue_sticking_out,  //  3
        R.drawable.ri_smiley_surprised,            //  4
        R.drawable.ri_smiley_kissing,              //  5
        R.drawable.ri_smiley_yelling,              //  6
        R.drawable.ri_smiley_cool,                 //  7
        R.drawable.ri_smiley_money_mouth,          //  8
        R.drawable.ri_smiley_foot_in_mouth,        //  9
        R.drawable.ri_smiley_embarrassed,          //  10
        R.drawable.ri_smiley_angel,                //  11
        R.drawable.ri_smiley_undecided,            //  12
        R.drawable.ri_smiley_crying,               //  13
        R.drawable.ri_smiley_lips_are_sealed,      //  14
        R.drawable.ri_smiley_laughing,             //  15
        R.drawable.ri_smiley_wtf                   //  16
    };
    
    private static final int DEFAULT_SMILEY_TEXTS = R.array.default_smiley_texts;
    
    private static final int DEFAULT_SMILEY_NAMES = R.array.default_smiley_names;
    /**
     * Constructor
     * 
     * @param context
     */
    public Smileys(Context context) {
    	String[] smilies = context.getResources().getStringArray(DEFAULT_SMILEY_TEXTS);
		int[] smileyResIds = DEFAULT_SMILEY_RES_IDS;
    	
        for (int i = 0; i < smilies.length; i++) {
            TrieNode.addToTrie(smileys, smilies[i], "");
            mSmileyToRes.put(smilies[i], smileyResIds[i]);
        }
    }

    /**
     * Looks up the resource id of a given smiley. 
     * @param smiley The smiley to look up.
     * @return the resource id of the specified smiley, or -1 if no resource
     *         id is associated with it.  
     */
    public int getSmileyRes(String smiley) {
        Integer i = mSmileyToRes.get(smiley);
        if (i == null) {
            return -1;
        }
        return i.intValue();
    }

    private final TrieNode smileys = new TrieNode();

    public Set<String> getSchemes() {
        return null;
    }

    public TrieNode getDomainSuffixes() {
        return null;
    }

    public TrieNode getSmileys() {
        return smileys;
    }

    public TrieNode getAcronyms() {
        return null;
    }
    
    /**
     * Show a list of smileys
     */
    public static void showSmileyDialog(Context context, final EditText textEdit, final Resources resources, String title) {
        int[] icons = DEFAULT_SMILEY_RES_IDS;
        String[] names = resources.getStringArray(DEFAULT_SMILEY_NAMES);
        final String[] texts = resources.getStringArray(DEFAULT_SMILEY_TEXTS);

        final int N = names.length;

        List<Map<String, ?>> entries = new ArrayList<Map<String, ?>>();
        for (int i = 0; i < N; i++) {
            // We might have different ASCII for the same icon, skip it if
            // the icon is already added.
            boolean added = false;
            for (int j = 0; j < i; j++) {
                if (icons[i] == icons[j]) {
                    added = true;
                    break;
                }
            }
            if (!added) {
                HashMap<String, Object> entry = new HashMap<String, Object>();
                entry. put("icon", icons[i]);
                entry. put("name", names[i]);
                entry.put("text", texts[i]);
                entries.add(entry);
            }
        }

        final SimpleAdapter a = new SimpleAdapter(
                context,
                entries,
                R.layout.utils_smiley_menu_item,
                new String[] {"icon", "name", "text"},
                new int[] {R.id.smiley_icon, R.id.smiley_name, R.id.smiley_text});
        SimpleAdapter.ViewBinder viewBinder = new SimpleAdapter.ViewBinder() {
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                if (view instanceof ImageView) {
                    Drawable img = resources.getDrawable((Integer)data);
                    ((ImageView)view).setImageDrawable(img);
                    return true;
                }
                return false;
            }
        };
        a.setViewBinder(viewBinder);

        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle(title);
        b.setCancelable(true);
        b.setAdapter(a, new DialogInterface.OnClickListener() {
            public final void onClick(DialogInterface dialog, int which) {
                @SuppressWarnings("unchecked")
				HashMap<String, Object> item = (HashMap<String, Object>)a.getItem(which);
                textEdit.append((String)item.get("text"));
            }
        });

        AlertDialog dialog = b.create();
        dialog.show();
    }

}
