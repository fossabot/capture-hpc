package capture;

import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.LinkedBlockingDeque;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class UrlsController extends Observable implements EventObserver, Observer {
    private LinkedBlockingDeque<Url> urlQueue;
    private LinkedList<Url> visitingList;
    private LinkedList<Url> visitedList;
    private LinkedList<Url> errorUrlList;
    private UrlFactory urlFactory;

    private UrlsController() {
        visitingList = new LinkedList<Url>();
        visitedList = new LinkedList<Url>();
        errorUrlList = new LinkedList<Url>();
        urlFactory = new UrlFactory();
        urlQueue = new LinkedBlockingDeque<Url>();
        EventsController.getInstance().addEventObserver("url", this);
    }

    private final static UrlsController instance = new UrlsController();

    public static UrlsController getInstance() {
        return instance;
    }

    public int getQueueSize() {
        return urlQueue.size();
    }
    public void update(Element event) {
        if (event.name.equals("url")) {
            if (event.attributes.containsKey("add")) {
                addUrlEvent(event);
            } else if (event.attributes.containsKey("remove")) {
                removeUrlEvent(event);
            }
        }
    }

    private void addUrlEvent(Element event) {

        try {
            Url url = urlFactory.getUrl(event);
            this.addUrl(url);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

    private void addUrl(Url url) {
        url.addObserver(this);
        urlQueue.addLast(url);
        this.setChanged();
        this.notifyObservers(url);
    }

    private void removeUrlEvent(Element event) {
        try {
            Url url = urlFactory.getUrl(event);
            if (urlQueue.contains(url)) {
                urlQueue.remove(url);
                this.setChanged();
                this.notifyObservers(url);
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

    public Url takeUrl() throws InterruptedException {
        return urlQueue.take();
    }

    public void update(Observable arg0, Object arg1) {
        Url url = (Url) arg0;
        if (url.getUrlState() == URL_STATE.VISITING) {
            visitingList.add(url);
        } else if (url.getUrlState() == URL_STATE.VISITED) {
            visitingList.remove(url);
            visitedList.add(url);
        } else if (url.getUrlState() == URL_STATE.ERROR) {
            visitingList.remove(url);

            String date = url.getVisitFinishTime();
            String urlString = url.getUrl();
            String clientProgram = url.getClientProgram();
            int errorCount = url.getErrorCount();
            int visitTime = url.getVisitTime();
            ERROR_CODES majorErrorCode = url.getMajorErrorCode();
            long minorErrorCode = url.getMinorErrorCode();
            if(url.getInitialGroup()) {
                Logger.getInstance().writeToErrorLog("\"" + date + "\",\"error:" + majorErrorCode + "-" + minorErrorCode + "\",\"" + url.getGroupID() + "\",\"" + urlString + "\",\""  + clientProgram + "\",\"" + visitTime + "\"");
                errorUrlList.add(url);
            }
        }
    }
}