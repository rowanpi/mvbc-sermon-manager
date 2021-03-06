package org.churchsource.sermon;

import java.net.MalformedURLException;
import java.util.List;

import org.churchsource.sermon.book.Book;
import org.churchsource.sermon.book.BookFactory;
import org.churchsource.sermon.book.BookRepository;
import org.churchsource.sermon.book.wp.WPSermonBook;
import org.churchsource.sermon.googlesheet.planner.Entry;
import org.churchsource.sermon.googlesheet.planner.GoogleSheetSermonPlanner;
import org.churchsource.sermon.media.MediaItem;
import org.churchsource.sermon.media.MediaItemFactory;
import org.churchsource.sermon.media.MediaItemRepository;
import org.churchsource.sermon.media.MediaItemService;
import org.churchsource.sermon.media.wp.WPMedia;
import org.churchsource.sermon.mvbcuploaded.MvbcUploadedSermon;
import org.churchsource.sermon.mvbcuploaded.MvbcUploadedSermonFactory;
import org.churchsource.sermon.mvbcuploaded.MvbcUploadedSermonRepository;
import org.churchsource.sermon.mvbcuploaded.wp.WPMvbcUploadedSermon;
import org.churchsource.sermon.preacher.Preacher;
import org.churchsource.sermon.preacher.PreacherFactory;
import org.churchsource.sermon.preacher.PreacherRepository;
import org.churchsource.sermon.preacher.wp.WPPreacher;
import org.churchsource.sermon.recordings.RecordingService;
import org.churchsource.sermon.series.Series;
import org.churchsource.sermon.series.SeriesFactory;
import org.churchsource.sermon.series.SeriesRepository;
import org.churchsource.sermon.series.wp.WPSermonSeries;
import org.churchsource.sermon.sermonplanner.SermonPlannerFactory;
import org.churchsource.sermon.sermonplanner.SermonPlannerItem;
import org.churchsource.sermon.sermonplanner.SermonPlannerRepository;
import org.churchsource.sermon.service.ServiceType;
import org.churchsource.sermon.service.ServiceTypeFactory;
import org.churchsource.sermon.service.ServiceTypeRepository;
import org.churchsource.sermon.service.wp.WPServiceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("wpsermonmanager")
@Slf4j
public class WPSermonManagerController {

  private static final String SERMON_PLANNER_LINK 
    = "https://spreadsheets.google.com/feeds/list/19fmsMGR_yNdaGO3vXZHpEaPe6W3x279orFcbXwx7UtQ/2/public/values?alt=json";

  @Autowired
  private PreacherRepository preacherRepository;

  @Autowired
  private PreacherFactory preacherFactory;

  @Autowired
  private ServiceTypeRepository serviceTypeRepository;

  @Autowired
  private ServiceTypeFactory serviceTypeFactory;
  
  @Autowired
  private SeriesRepository seriesRepository;

  @Autowired
  private SeriesFactory seriesFactory;

  @Autowired
  private BookRepository bookRepository;

  @Autowired
  private BookFactory bookFactory;

  @Autowired
  private SermonPlannerRepository sermonPlannerRepository;

  @Autowired
  private SermonPlannerFactory sermonPlannerFactory;

  @Autowired
  private MvbcUploadedSermonFactory mvbcUploadedSermonFactory;

  @Autowired
  private MvbcUploadedSermonRepository mvbcUploadedSermonRepository;

  @Autowired
  private MediaItemFactory mediaItemFactory;

  @Autowired
  private MediaItemRepository mediaItemRepository;

  @Autowired
  private MediaItemService mediaItemService; 

  @Autowired
  RecordingService recordingService;
  
  @GetMapping("/preacher/{id}")
  public Preacher getPreacher(@PathVariable Long id) {
    return preacherRepository.getPreacherById(id);
  }

  @GetMapping("/preachers")
  public List<Preacher> getAllPreacher() {
    return preacherRepository.getAllPreachers();
  }

  @RequestMapping(value = "/sync", method = RequestMethod.POST)
  public String sync() throws Exception {
    RestTemplate restTemplate = new RestTemplate();
    syncSermonPlanner(restTemplate);
    syncPreachers(restTemplate);
    syncServiceTypes(restTemplate);
    syncSermonSeries(restTemplate);
    syncSermonBooks(restTemplate);
    syncUploadedSermons(restTemplate);
    syncMediaItems(restTemplate);
    uploadAnyNewImages(restTemplate);
    uploadAndSyncAnyNewRecordings(restTemplate);
    return null;
  }

  private void uploadAndSyncAnyNewRecordings(RestTemplate restTemplate) throws MalformedURLException {
    recordingService.uploadAllNewRecordings();
    syncUploadedSermons(restTemplate);
  }

  private void syncSermonSeries(RestTemplate restTemplate) {
    WPSermonSeries[] wpSermonSeries = restTemplate.getForObject("https://www.mountainviewbaptist.co.za/wp-json/wp/v2/wpfc_sermon_series?per_page=100", WPSermonSeries[].class);
    for(WPSermonSeries aWpSermonSeries : wpSermonSeries) {
      Series aSeries = seriesFactory.createEntity(aWpSermonSeries);
      seriesRepository.saveNewOrUpdateExistingSeries(aSeries);
      log.info(wpSermonSeries.toString());
    }
  }

  private void syncServiceTypes(RestTemplate restTemplate) {
    WPServiceType[] wpServiceTypes = restTemplate.getForObject("https://www.mountainviewbaptist.co.za/wp-json/wp/v2/wpfc_service_type?per_page=100", WPServiceType[].class);
    for(WPServiceType wpServiceType : wpServiceTypes) {
      ServiceType aServiceType = serviceTypeFactory.createEntity(wpServiceType);
      serviceTypeRepository.saveNewOrUpdateExistingServiceType(aServiceType);
      log.info(wpServiceType.toString());
    }
  }

  private void syncPreachers(RestTemplate restTemplate) {
    WPPreacher[] wpPreachers = restTemplate.getForObject("https://www.mountainviewbaptist.co.za/wp-json/wp/v2/wpfc_preacher?per_page=100", WPPreacher[].class);
    for(WPPreacher wpPreacher: wpPreachers) {
      Preacher aPreacherEntity = preacherFactory.createEntity(wpPreacher);
      preacherRepository.saveNewOrUpdateExistingPreacher(aPreacherEntity);
      log.info(wpPreacher.toString());
    }
  }

  private void syncSermonBooks(RestTemplate restTemplate) {
    WPSermonBook[] wpBooks = restTemplate.getForObject("https://www.mountainviewbaptist.co.za/wp-json/wp/v2/wpfc_bible_book?per_page=100", WPSermonBook[].class);
    for(WPSermonBook wpSermonBook: wpBooks) {
      Book aBook = bookFactory.createEntity(wpSermonBook);
      bookRepository.saveNewOrUpdateExistingBook(aBook);
      log.info(wpSermonBook.toString());
    }
  }

  private void syncUploadedSermons(RestTemplate restTemplate) throws MalformedURLException {
    WPMvbcUploadedSermon[] wpSermons = restTemplate.getForObject("https://www.mountainviewbaptist.co.za/wp-json/wp/v2/wpfc_sermon?per_page=100", WPMvbcUploadedSermon[].class);
    for(WPMvbcUploadedSermon wpSermon: wpSermons) {
      MvbcUploadedSermon mvbcUploadedSermon = mvbcUploadedSermonFactory.createEntity(wpSermon);
      mvbcUploadedSermonRepository.saveNewOrUpdateExistingUploadedSermon(mvbcUploadedSermon);
      log.info(mvbcUploadedSermon.toString());
    }
  }

  private void syncSermonPlanner(RestTemplate restTemplate) throws Exception {
    GoogleSheetSermonPlanner sermonPlanner = restTemplate.getForObject(SERMON_PLANNER_LINK, GoogleSheetSermonPlanner.class);
    for(Entry plannerEntry: sermonPlanner.getFeed().getEntry()) {
      SermonPlannerItem item = sermonPlannerFactory.createSermonPlannerItem(plannerEntry);
      sermonPlannerRepository.saveOrUpdateSermonPlanningItem(item);
      log.info(item.toString());
    }
  }

  private void syncMediaItems(RestTemplate restTemplate) {
    WPMedia[] wpMedias = restTemplate.getForObject("https://www.mountainviewbaptist.co.za/wp-json/wp/v2/media?per_page=100", WPMedia[].class);
    for(WPMedia wpMedia : wpMedias) {
      MediaItem aMediaItem = mediaItemFactory.createEntity(wpMedia);
      mediaItemRepository.saveNewOrUpdateExistingMediaItem(aMediaItem);
      log.info(aMediaItem.toString());
    }
  }
  
  private void uploadAnyNewImages(RestTemplate restTemplate) {
    mediaItemService.uploadAllNewSermonImages();
    //TODO should really do this in the service from the response returned. but too lazy now. 
    syncMediaItems(restTemplate);
  }
}
