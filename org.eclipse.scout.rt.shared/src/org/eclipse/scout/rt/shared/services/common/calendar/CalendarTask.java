/*******************************************************************************
 * Copyright (c) 2010 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.shared.services.common.calendar;

import java.util.Date;
import java.util.Map;

import org.eclipse.scout.commons.DateUtility;

public class CalendarTask extends AbstractCalendarItem implements ICalendarTask, java.io.Serializable {
  private static final long serialVersionUID = 0L;

  private Long m_responsibleId;
  private Date m_start;
  private Date m_due;
  private Date m_complete;

  public CalendarTask() {
    super();
  }

  /**
   * @param id
   * @param responsibleId
   * @param startDate
   * @param dueDate
   * @param completeDate
   * @param subject
   * @param body
   * @param color
   */
  public CalendarTask(long id, Long responsibleId, Date startDate, Date dueDate, Date completeDate, String subject, String body, String color) {
    setId(id);
    setResponsibleId(id);
    setStart(startDate);
    setDue(dueDate);
    setComplete(completeDate);
    setSubject(subject);
    setBody(body);
    setColor(color);
  }

  /**
   * @param id
   * @param responsibleId
   * @param startDate
   * @param dueDate
   * @param completeDate
   * @param subject
   * @param body
   * @param color
   */
  public CalendarTask(Object[] data) {
    if (data != null) {
      for (int i = 0; i < data.length; i++) {
        if (data[i] != null) {
          switch (i) {
            case 0: {
              setId(((Number) data[i]).longValue());
              break;
            }
            case 1: {
              setResponsibleId(((Number) data[i]).longValue());
              break;
            }
            case 2: {
              setStart((Date) data[i]);
              break;
            }
            case 3: {
              setDue((Date) data[i]);
              break;
            }
            case 4: {
              setComplete((Date) data[i]);
              break;
            }
            case 5: {
              setSubject((String) data[i]);
              break;
            }
            case 6: {
              setBody((String) data[i]);
              break;
            }
            case 7: {
              setColor((String) data[i]);
              break;
            }
          }
        }
      }
    }
  }

  @Override
  public boolean isIntersecting(Date minDate, Date maxDate) {
    Date d = m_due;
    if (d == null) d = m_complete;
    return DateUtility.intersects(m_start, d, minDate, maxDate);
  }

  @Override
  public Long getResponsibleId() {
    return m_responsibleId;
  }

  @Override
  public void setResponsibleId(Long n) {
    m_responsibleId = n;
  }

  @Override
  public Date getStart() {
    return m_start;
  }

  @Override
  public void setStart(Date a) {
    m_start = a;
  }

  @Override
  public Date getDue() {
    return m_due;
  }

  @Override
  public void setDue(Date a) {
    m_due = a;
  }

  @Override
  public Date getComplete() {
    return m_complete;
  }

  @Override
  public void setComplete(Date a) {
    m_complete = a;
  }

  @Override
  public ICalendarItem copy() {
    CalendarTask t = (CalendarTask) super.copy();
    t.m_responsibleId = this.m_responsibleId;
    t.m_start = this.m_start;
    t.m_due = this.m_due;
    t.m_complete = this.m_complete;
    return t;
  }

  @Override
  protected void dumpState(Map<String, Object> attributes) {
    super.dumpState(attributes);
    if (m_start != null) attributes.put("start", getDumpDateFormat().format(m_start));
    if (m_due != null) attributes.put("due", getDumpDateFormat().format(m_due));
    if (m_complete != null) attributes.put("complete", getDumpDateFormat().format(m_complete));
  }
}
