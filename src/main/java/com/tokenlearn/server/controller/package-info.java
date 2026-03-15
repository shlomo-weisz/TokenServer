/**
 * REST controllers for the public API.
 *
 * <p>Controllers stay intentionally thin: they read HTTP input, resolve the
 * authenticated user from Spring Security, delegate to a service, and wrap the
 * result with the shared API response envelope.
 */
package com.tokenlearn.server.controller;
