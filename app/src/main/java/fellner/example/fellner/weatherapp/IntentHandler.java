package fellner.example.fellner.weatherapp;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Point;
import android.view.Display;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import WeatherParser.DailyWeather;
import WeatherParser.FetchWeatherData;
import WeatherParser.ThreeHourlyWeather;

/**
 * Created by Yoshi on 21.01.2016.
 */
public class IntentHandler extends IntentService {
    static ChartActivity ca;

    public IntentHandler(){
        super("IntentHandler");
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        String dataString = workIntent.getDataString();

        Display display = ca.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        ca.width = size.x;
        ca.height = size.y;

        DailyWeather dw;
        ArrayList<ThreeHourlyWeather> thw = null;

        //this test if there even is an internet connection to openweathermap.org
        try {
            InetAddress.getByName("openweathermap.org");
        } catch (IOException e) {
            ca.loadMain(ca.findViewById(R.id.chartView));
            Toast.makeText(getApplicationContext(), "No Connection Available", Toast.LENGTH_LONG).show();
        }


        String appid = "";
        try {
            //Saves Document using Jsoup from URL and saves all a tags containing href
            Document doc = Jsoup.connect("http://openweathermap.org/current").get();
            Elements aTags = doc.select("a[href]");

            String wholeappid = "";

            //loops through the a tags and stops when the href contains an appid
            for (Element aTag : aTags) {
                String aTagString = aTag.attr("abs:href");

                if(aTagString.contains("&appid=")){
                    wholeappid = aTagString;
                    break;
                }
            }

            appid = wholeappid.split("&appid=")[1];
            String url = "http://api.openweathermap.org/data/2.5/forecast?q="+ca.city+"&mode=xml&appid="+appid;


            dw = FetchWeatherData.fetchIt(url);
            thw = dw.getThreeHourlyWeatherData();
        }catch(Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),"City non existent", Toast.LENGTH_LONG).show();
            ca.loadMain(ca.findViewById(R.id.chartView));
        }
        if (thw != null) {
            ca.temperatures = new float[thw.size()];
            ca.timeOfDay = new String[thw.size()];
            for (int i = 0; i < 8; i++) {
                ca.temperatures[i] = Float.parseFloat(thw.get(i).getTemperature_celsius());
                ca.timeOfDay[i] = thw.get(i).getStarting_hour().substring(0, 5);
            }

        }



        NodeList nodeList = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = null;
            db = dbf.newDocumentBuilder();
            org.w3c.dom.Document document = db.parse("http://api.openweathermap.org/data/2.5/weather?q="+ca.city+"&mode=xml&appid="+appid);

            nodeList = document.getDocumentElement().getChildNodes();
            ca.currentTemperatureText = Double.toString(Math.ceil((Double.parseDouble(nodeList.item(1).getAttributes().item(0).getNodeValue()) - 273.15) * 100) / 100) + "C°";
            ca.currentClimateText = nodeList.item(8).getAttributes().item(1).getNodeValue();

        } catch (Exception e) {
            e.printStackTrace();
        }


        ca.climateIconID = getResources().getIdentifier("image_"+nodeList.item(8).getAttributes().item(2).getNodeValue(), "drawable", getPackageName());

        ca.loadContent();
    }
}