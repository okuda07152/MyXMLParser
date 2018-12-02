package com.example.kurodai0715.myxmlparser;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import com.example.kurodai0715.myxmlparser.StackOverflowXmlParser.Entry;

public class NetworkActivity extends AppCompatActivity {
    public static final String WIFI = "Wi-Fi";
    public static final String ANY = "Any";
    private static final String URL =
            "https://stackoverflow.com/feeds/tag?tagnames=android&sort=newest";

    // Wi-Fi接続があるかどうか。
    private static boolean wifiConnected = true;
    // モバイル接続があるかどうか。
    private static boolean mobileConnected = false;
    // 表示がリフレッシュされるべきかどうか。
    public static boolean refreshDisplay = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // stackoverflow.comからXMLフィードをダウンロードする為にAsyncTaskを使用します。
        new DownloadXmlTask().execute(URL);

    }

    // stackoverflow.comからXMLフィードをダウンロードする為に使用されるAsyncTaskの実装
    private class DownloadXmlTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                return loadXmlFromNetwork(urls[0]);
            } catch (IOException e) {
                return getResources().getString(R.string.connection_error);
            } catch (XmlPullParserException e) {
                return getResources().getString(R.string.xml_error);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            setContentView(R.layout.main);
            // WebViewでUI内にHTML文字列を表示します。
            WebView myWebView = findViewById(R.id.webview);
            myWebView.loadData(result, "text/html", null);
        }
    }

    /**
     * stackoverflow.comからXMLをダウンロードし、それを解析して、HTMLマークアップと結合します。
     * HTMLの文字列を返します。
     *
     * @param urlString XML形式のデータを取得するためのURL
     * @return WebViewに表示するHTMLデータの文字列
     * @throws XmlPullParserException
     * @throws IOException
     */
    private String loadXmlFromNetwork(String urlString)
            throws XmlPullParserException, IOException {
        InputStream stream = null;
        // パーサーをインスタンス化します。
        StackOverflowXmlParser stackOverflowXmlParser = new StackOverflowXmlParser();
        List<Entry> entries = null;
        String title = null;
        String url = null;
        String summary = null;
        // 現在日時を取得
        Calendar rightNow = Calendar.getInstance();
        DateFormat formatter = new SimpleDateFormat("MMM dd h:mmaa");

        // ユーザーが環境設定で概要テキストを含むと設定したかどうか確認します。
        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(this);
        boolean pref = sharedPrefs.getBoolean("summaryPref", false);

        StringBuilder htmlString = new StringBuilder();

        // WebViewのTOPに表示するページタイトルと更新日時をViewにセットする。
        htmlString.append("<h3>" + getResources()
                .getString(R.string.page_title) + "</h3>");
        htmlString.append("<em>" + getResources()
                .getString(R.string.updated) + " " +
                formatter.format(rightNow.getTime()) + "</em>");

        try {
            stream = downloadUrl(urlString);
            entries = stackOverflowXmlParser.parse(stream);
            // アプリがInputStreamを使用して完了した後にそれがクローズされているか確認します。
            // finished using it.
        } finally {
            if (stream != null) {
                stream.close();
            }
        }

        // StackOverflowXmlParserはEntryオブジェクトの一覧(「entries」と呼ばれる)を返します。
        // 各EntryオブジェクトはXMLフィード内の単一の投稿を表します。
        // このセクションは各エントリをHTMLマークアップと結合する為にエントリの一覧を処理します。
        // 各エントリは任意で概要テキストを含むリンクとしてUI内で表示されます。
        for (Entry entry : entries) {
            htmlString.append("<p><a href='");
            htmlString.append(entry.link);
            htmlString.append("'>" + entry.title + "</a></p>");
            // ユーザーが環境設定で概要テキストを含むようにセットしている場合、
            // それを表示に加えます。
            if (pref) {
                htmlString.append(entry.summary);
            }
        }
        return htmlString.toString();
    }

    // 与えられたURLの文字列表現で接続をセットアップし入力ストリームを取得します
    private InputStream downloadUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000 /* ミリセカンド */);
        conn.setConnectTimeout(15000 /* ミリセカンド */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        // 問い合わせを開始します。
        conn.connect();
        return conn.getInputStream();
    }

}
