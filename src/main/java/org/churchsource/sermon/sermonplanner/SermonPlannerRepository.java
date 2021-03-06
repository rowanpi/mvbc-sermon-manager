package org.churchsource.sermon.sermonplanner;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.churchsource.sermon.model.AbstractRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(noRollbackFor=Exception.class)
public class SermonPlannerRepository extends AbstractRepository<SermonPlannerItem> {

  @PersistenceContext
  private EntityManager em;

  public List<SermonPlannerItem> getAllSermonPlannerItems() {
    return em.createNamedQuery(SermonPlannerNamedQueryConstants.NAME_FIND_ALL_SERMON_PLANNER_ITEMS, SermonPlannerItem.class).getResultList();
  }

  public SermonPlannerItem getSermonPlannerById(Long id) {
    List<SermonPlannerItem> sermonPlannerItems = em.createNamedQuery(SermonPlannerNamedQueryConstants.NAME_FIND_SERMON_PLANNER_ITEM_BY_ID, SermonPlannerItem.class).setParameter("id", id)
        .getResultList();
    return sermonPlannerItems == null || sermonPlannerItems.isEmpty() ? null : sermonPlannerItems.get(0); 
  }
  
  public SermonPlannerItem getSermonPlannerItemByDateAndServiceTime(Date date, String amOrPm) {
    List<SermonPlannerItem> sermonPlannerItems = em.createNamedQuery(SermonPlannerNamedQueryConstants.NAME_FIND_SERMON_PLANNER_ITEM_BY_DATE_AND_TIME, SermonPlannerItem.class)
        .setParameter("date", date)
        .setParameter("amOrPm", SermonServiceAmPm.valueOf(amOrPm.toUpperCase()))
        .getResultList();
    return sermonPlannerItems == null || sermonPlannerItems.isEmpty() ? null : sermonPlannerItems.get(0); 
  }

  public SermonPlannerItem getSermonPlannerByUniqueGoogleCode(String code) {
    List<SermonPlannerItem> sermonPlannerItems = em.createNamedQuery(SermonPlannerNamedQueryConstants.NAME_FIND_SERMON_PLANNER_ITEM_BY_UNIQUE_GOOGLE_ID, 
        SermonPlannerItem.class).setParameter("code", code)
        .getResultList();
    return sermonPlannerItems == null || sermonPlannerItems.isEmpty() ? null : sermonPlannerItems.get(0); 
  }

  public void saveOrUpdateSermonPlanningItem(SermonPlannerItem aSermonPlannerItem) {
    SermonPlannerItem sermonPlannerItemFromDB = getSermonPlannerByUniqueGoogleCode(aSermonPlannerItem.getUniqueGoogleId());
    if(sermonPlannerItemFromDB != null) {
      aSermonPlannerItem.setId(sermonPlannerItemFromDB.getId());
      update(aSermonPlannerItem);
    } else {
      save(aSermonPlannerItem);
    }
    
  }

  public void deleteAllSermonPlanningItems() {
    em.createQuery("DELETE FROM SermonPlannerItem").executeUpdate();
    em.flush();
  }
}