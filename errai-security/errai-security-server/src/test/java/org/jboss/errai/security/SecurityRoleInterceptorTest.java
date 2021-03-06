/**
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.errai.security;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.interceptor.InvocationContext;

import org.jboss.errai.security.client.shared.ServiceInterface;
import org.jboss.errai.security.res.Service;
import org.jboss.errai.security.server.ServerSecurityRoleInterceptor;
import org.jboss.errai.security.shared.api.identity.Role;
import org.jboss.errai.security.shared.api.identity.User;
import org.jboss.errai.security.shared.exception.UnauthenticatedException;
import org.jboss.errai.security.shared.exception.UnauthorizedException;
import org.jboss.errai.security.shared.service.AuthenticationService;
import org.junit.Before;
import org.junit.Test;

/**
 * @author edewit@redhat.com
 */
public class SecurityRoleInterceptorTest {
  private AuthenticationService authenticationService;
  private ServerSecurityRoleInterceptor interceptor;

  @Before
  public void setUp() throws Exception {
    authenticationService = mock(AuthenticationService.class);
    interceptor = new ServerSecurityRoleInterceptor(authenticationService);
  }

  @Test
  public void shouldVerifyUserInRole() throws Exception {
    // given
    InvocationContext context = mock(InvocationContext.class);

    // when
    when(context.getTarget()).thenReturn(new Service());
    when(context.getMethod()).thenReturn(getAnnotatedServiceMethod());
    final User user = new User();
    final Set<Role> roles = new HashSet<Role>();
    roles.addAll(Arrays.asList(new Role("admin"), new Role("user")));
    user.setRoles(roles);
    when(authenticationService.getUser()).thenReturn(user);
    interceptor.aroundInvoke(context);

    // then
    verify(context).proceed();
  }

  @Test(expected = UnauthorizedException.class)
  public void shouldThrowExceptionWhenUserNotInRole() throws Exception {
    // given
    InvocationContext context = mock(InvocationContext.class);

    // when
    invokeTest(context, new Service());

    // then
    fail("security exception should have been thrown");
  }

  @Test(expected = UnauthenticatedException.class)
  public void shouldThrowExceptionWhenNotLoggedIn() throws Exception {
    // given
    InvocationContext context = mock(InvocationContext.class);

    // when
    when(context.getTarget()).thenReturn(new Service());
    when(context.getMethod()).thenReturn(getAnnotatedServiceMethod());
    when(authenticationService.getUser()).thenReturn(null);
    interceptor.aroundInvoke(context);

    fail("exception shoudl have been thrown");
  }

  @Test(expected = UnauthorizedException.class)
  public void shouldFindMethodWhenNoInterface() throws Exception {
    // given
    InvocationContext context = mock(InvocationContext.class);

    // when
    invokeTest(context, this);

    // then
    fail("security exception should have been thrown");
  }

  private void invokeTest(InvocationContext context, Object service) throws Exception {
    when(context.getTarget()).thenReturn(service);
    when(context.getMethod()).thenReturn(getAnnotatedServiceMethod());
    final User user = new User();
    when(authenticationService.getUser()).thenReturn(user);
    interceptor.aroundInvoke(context);
  }

  @Test(expected = UnauthorizedException.class)
  public void shouldFindMethodWhenOnInterface() throws Exception {
    // given
    InvocationContext context = mock(InvocationContext.class);

    // when
    when(context.getTarget()).thenReturn(new Service());
    when(context.getMethod()).thenReturn(Service.class.getMethod("annotatedServiceMethod"));
    final User user = new User();
    when(authenticationService.getUser()).thenReturn(user);
    interceptor.aroundInvoke(context);

    // then
    fail("security exception should have been thrown");
  }

  private Method getAnnotatedServiceMethod() throws NoSuchMethodException {
    return ServiceInterface.class.getMethod("annotatedServiceMethod");
  }
}
