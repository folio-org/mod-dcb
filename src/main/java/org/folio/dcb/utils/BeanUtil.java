package org.folio.dcb.utils;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
public class BeanUtil implements ApplicationContextAware {

  private ApplicationContext context;

  @Override
  public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
    this.context = applicationContext;
  }

  public <T> T getBean(Class<T> beanClass) {
    return context.getBean(beanClass);
  }
}
