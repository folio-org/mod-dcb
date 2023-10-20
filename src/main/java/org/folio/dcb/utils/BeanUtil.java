package org.folio.dcb.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

@Service
public class BeanUtil implements ApplicationContextAware {

  @Autowired
  private ApplicationContext context;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.context = applicationContext;
  }

  public <T> T getBean(Class<T> beanClass) {
    return context.getBean(beanClass);
  }
}
