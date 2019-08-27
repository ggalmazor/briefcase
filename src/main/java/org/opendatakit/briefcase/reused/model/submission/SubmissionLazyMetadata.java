/*
 * Copyright (C) 2018 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.briefcase.reused.model.submission;

import static java.util.stream.Collectors.toList;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.api.Iso8601Helpers;
import org.opendatakit.briefcase.reused.api.Optionals;
import org.opendatakit.briefcase.reused.model.XmlElement;

/**
 * This class holds a form's metadata. Instances of this class are
 * generated while parsing submissions and they are used to read
 * each value only once.
 * <p>
 * All its members are lazily evaluated to avoid unnecessary parsing.
 */
public class SubmissionLazyMetadata {
  private final XmlElement root;
  // All these members are not final because they're lazily evaluated
  private String formId;
  // TODO Make explicit that these members are lazily initialized
  private Optional<String> instanceId;
  private Optional<String> version;
  private Optional<OffsetDateTime> submissionDate;
  private Optional<String> encryptedXmlFile;
  private Optional<String> base64EncryptedKey;
  private Optional<String> encryptedSignature;
  private List<String> mediaNames;

  /**
   * Main constructor for {@link SubmissionLazyMetadata} class. It takes
   * an {@link XmlElement} to act as the root element which will be
   * queries for the different values this class can offer.
   */
  public SubmissionLazyMetadata(XmlElement root) {
    this.root = root;
  }

  /**
   * Returns the submission date, located at the root node's "submissionDate" attribute.
   * <p>
   * The value gets mapped to an {@link OffsetDateTime}
   */
  public Optional<OffsetDateTime> getSubmissionDate() {
    if (submissionDate == null)
      submissionDate = root.getAttributeValue("submissionDate")
          .map(Iso8601Helpers::parseDateTime);
    return submissionDate;
  }

  /**
   * Returns this submission's instance ID, which is taken from the &lt;instanceID&gt;
   * element's value, or from the root node's "instanceID" attribute.
   */
  public Optional<String> getInstanceId() {
    if (instanceId == null)
      instanceId = Optionals.race(
          root.findElement("instanceID").flatMap(XmlElement::maybeValue),
          root.getAttributeValue("instanceID")
      );
    return instanceId;
  }

  /**
   * Returns this submission's form ID, which is taken from the root node's "id" or
   * "xmlns" attribute.
   */
  public String getFormId() {
    if (formId == null)
      formId = Optionals.race(
          root.getAttributeValue("id"),
          root.getAttributeValue("xmlns")
      ).orElseThrow(() -> new BriefcaseException("Unable to extract form id"));
    return formId;
  }

  /**
   * Return this submission's version, which is taken from the root node's
   * "version" attribute.
   */
  public Optional<String> getVersion() {
    if (version == null)
      version = root.getAttributeValue("version");
    return version;
  }

  /**
   * Return the base64 encoded encryption key from the &lt;base64EncryptedKey&gt;
   * element's value.
   */
  Optional<String> getBase64EncryptedKey() {
    if (base64EncryptedKey == null)
      base64EncryptedKey = root.findElement("base64EncryptedKey").flatMap(XmlElement::maybeValue);
    return base64EncryptedKey;
  }

  /**
   * Returns the list of media attachment file names, which are the values of
   * all the &lt;file&gt; children in the &lt;media&gt element
   */
  public List<String> getMediaNames() {
    if (mediaNames == null)
      mediaNames = root.findElements("media").stream()
          .flatMap(e -> e.findElements("file").stream())
          .map(XmlElement::maybeValue)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .collect(toList());
    return mediaNames;
  }

  /**
   * Return the file name of the encrypted submission file, taken from the
   * &lt;encryptedXmlFile&gt; element's value.
   */
  Optional<String> getEncryptedXmlFile() {
    if (encryptedXmlFile == null)
      encryptedXmlFile = root.findElement("encryptedXmlFile").flatMap(XmlElement::maybeValue);
    return encryptedXmlFile;
  }

  /**
   * Return the cryptographic signature of this submissions, taken from the
   * &lt;base64EncryptedElementSignature&gt; element's value.
   */
  Optional<String> getEncryptedSignature() {
    if (encryptedSignature == null)
      encryptedSignature = root.findElement("base64EncryptedElementSignature").flatMap(XmlElement::maybeValue);
    return encryptedSignature;
  }

  public SubmissionMetadata freeze(String instanceId, Path submissionFile) {
    SubmissionKey submissionKey = new SubmissionKey(
        getFormId(),
        getVersion(),
        instanceId
    );
    return new SubmissionMetadata(
        submissionKey,
        Optional.of(submissionFile),
        getSubmissionDate(),
        getEncryptedXmlFile().map(Paths::get),
        getBase64EncryptedKey(),
        getEncryptedSignature(),
        getMediaNames().stream().map(Paths::get).collect(toList())
    );
  }
}
