package com.autumncoding.stickman;

import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.autamncoding.stickman.R;

public class HelpView extends LinearLayout {	
	private Button m_links[] = null;
	
	public HelpView(Context context) {
		super(context);
		inflate(context, R.layout.help_layout, this);
		m_links = new Button[7];
		
		m_links[0] = (Button)findViewById(R.id.helpbtn1);
		m_links[1] = (Button)findViewById(R.id.helpbtn2);
		m_links[2] = (Button)findViewById(R.id.helpbtn3);
		m_links[3] = (Button)findViewById(R.id.helpbtn4);
		m_links[4] = (Button)findViewById(R.id.helpbtn5);
		m_links[5] = (Button)findViewById(R.id.helpbtn6);
		m_links[6] = (Button)findViewById(R.id.helpbtn7);
	}
	
	public void setMyClickListener(OnClickListener l) {
		for (Button v : m_links)
			v.setOnClickListener(l);
	}
	
	void UpdateTexts() {
		// TODO fill
		
	}

}
