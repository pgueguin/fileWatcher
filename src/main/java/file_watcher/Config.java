package file_watcher;

import java.util.List;

/**
 * Created by pierre on 29/04/14.
 */
public class Config {
    private Source source;
    private List<Destination> destinations;

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public List<Destination> getDestinations() {
        return destinations;
    }

    public void setDestinations(List<Destination> destinations) {
        this.destinations = destinations;
    }
}
