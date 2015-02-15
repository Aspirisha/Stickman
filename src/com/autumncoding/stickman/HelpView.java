package com.autumncoding.stickman;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.autamncoding.stickman.R;

public class HelpView extends LinearLayout {	
	private TextView m_links[] = null;
	
	public HelpView(Context context) {
		super(context);
		inflate(context, R.layout.help_layout, this);
		m_links = new TextView[7];
		
		m_links[0] = (TextView)findViewById(R.id.textView2);
		m_links[1] = (TextView)findViewById(R.id.textView1);
		m_links[2] = (TextView)findViewById(R.id.TextView01);
		m_links[3] = (TextView)findViewById(R.id.TextView02);
		m_links[4] = (TextView)findViewById(R.id.TextView03);
		m_links[5] = (TextView)findViewById(R.id.TextView04);
		m_links[6] = (TextView)findViewById(R.id.TextView05);
	}
	
	public void setMyClickListener(OnClickListener l) {
		for (TextView v : m_links)
			v.setOnClickListener(l);
	}
	
	void UpdateTexts() {
		// TODO fill
		
	}

}
