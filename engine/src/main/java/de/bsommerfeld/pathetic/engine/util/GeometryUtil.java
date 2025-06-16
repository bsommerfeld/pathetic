package de.bsommerfeld.pathetic.engine.util;

import de.bsommerfeld.pathetic.api.wrapper.PathPosition;

/**
 * Utility class for geometric calculations, such as computing the perpendicular distance from a
 * point to a line segment in 3D space. Supports both precise (with square root) and performance
 * (squared) variants.
 */
public class GeometryUtil {

  /**
   * Calculates the precise perpendicular (Euclidean) distance from a point P to the line segment AB
   * in 3D space.
   *
   * @param pointP the point for which the distance is calculated
   * @param pointA the start of the segment
   * @param pointB the end of the segment
   * @return the shortest distance from point P to segment AB
   */
  public static double perpendicularDistance(
      PathPosition pointP, PathPosition pointA, PathPosition pointB) {
    // Vector AB and AP
    double abX = pointB.getX() - pointA.getX();
    double abY = pointB.getY() - pointA.getY();
    double abZ = pointB.getZ() - pointA.getZ();

    double apX = pointP.getX() - pointA.getX();
    double apY = pointP.getY() - pointA.getY();
    double apZ = pointP.getZ() - pointA.getZ();

    // Cross product AB x AP
    double crossX = abY * apZ - abZ * apY;
    double crossY = abZ * apX - abX * apZ;
    double crossZ = abX * apY - abY * apX;
    double crossLength = Math.sqrt(crossX * crossX + crossY * crossY + crossZ * crossZ);

    // Length of AB
    double abLength = Math.sqrt(abX * abX + abY * abY + abZ * abZ);

    return abLength == 0.0 ? 0.0 : crossLength / abLength;
  }

  /**
   * Calculates the squared perpendicular distance from a point P to the line segment AB in 3D
   * space. This variant avoids the square root operation for better performance, which is useful if
   * only relative distances are needed.
   *
   * @param pointP the point for which the distance is calculated
   * @param pointA the start of the segment
   * @param pointB the end of the segment
   * @return the squared shortest distance from point P to segment AB
   */
  public static double squaredPerpendicularDistance(
      PathPosition pointP, PathPosition pointA, PathPosition pointB) {
    // Vector AB and AP
    double abX = pointB.getX() - pointA.getX();
    double abY = pointB.getY() - pointA.getY();
    double abZ = pointB.getZ() - pointA.getZ();

    double apX = pointP.getX() - pointA.getX();
    double apY = pointP.getY() - pointA.getY();
    double apZ = pointP.getZ() - pointA.getZ();

    // Cross product AB x AP
    double crossX = abY * apZ - abZ * apY;
    double crossY = abZ * apX - abX * apZ;
    double crossZ = abX * apY - abY * apX;
    double crossLengthSquared = crossX * crossX + crossY * crossY + crossZ * crossZ;

    // Squared length of AB
    double abLengthSquared = abX * abX + abY * abY + abZ * abZ;

    return abLengthSquared == 0.0 ? 0.0 : crossLengthSquared / abLengthSquared;
  }
}
